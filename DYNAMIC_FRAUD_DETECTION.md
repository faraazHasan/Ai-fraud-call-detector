# Dynamic Fraud Detection System

## Overview

This system implements targeted fraud detection notifications that are sent only to specific users based on their phone numbers as unique identifiers, instead of broadcasting to all connected users.

## Architecture

### Backend Components

#### 1. Enhanced WebSocket Management (`twilio.py`)

**WebSocketConnection Dataclass:**
```python
@dataclass
class WebSocketConnection:
    websocket: WebSocket
    user_id: str
    phone_number: str
```

**Key Data Structures:**
- `active_connections: Dict[str, WebSocketConnection]` - Maps user IDs to their WebSocket connections
- `call_to_user_mapping: Dict[str, str]` - Maps call SIDs to user IDs for targeted notifications

#### 2. User-Identified WebSocket Endpoint

**Endpoint:** `GET /api/ws?user_id={id}&phone_number={phone}`

**Features:**
- Accepts user identification parameters
- Creates user-specific connection tracking
- Handles call association messages
- Provides connection confirmation

#### 3. Call Association System

**WebSocket Association:**
```json
{
  "type": "associate_call",
  "call_sid": "CA1234567890",
  "user_id": "user_1234567890_1234567890",
  "phone_number": "+1234567890"
}
```

**HTTP Association Endpoint:** `POST /api/associate-call`
```json
{
  "call_sid": "CA1234567890",
  "user_id": "user_1234567890_1234567890",
  "phone_number": "+1234567890"
}
```

#### 4. Targeted Fraud Detection

**Enhanced `process_fraud_detection()` Function:**
1. Detects fraud using GPT-4
2. Looks up user ID associated with the call SID
3. Sends fraud alert only to the specific user
4. Falls back to broadcast if no mapping found

### Frontend Components

#### 1. Enhanced WebSocket Hook (`useWebSocket.ts`)

**New Interface:**
```typescript
interface UseWebSocketProps {
  url: string;
  userPhoneNumber?: string;
}
```

**Key Features:**
- Automatic user ID generation and persistence
- Phone number-based user identification
- Call association functionality
- Connection status tracking

**User ID Generation:**
```typescript
const userId = `user_${phoneHash}_${timestamp}`;
```

#### 2. Updated Call Management (`useOngoingCall.ts`)

**New Features:**
- Automatic call SID generation/retrieval
- Call association when recording starts
- WebSocket connection status monitoring
- Debug information exposure

#### 3. Debug Component (`FraudDetectionDebug.tsx`)

**Features:**
- Real-time connection status
- Server debug information
- Test controls for call association
- Fraud alert simulation

## User Flow

### 1. WebSocket Connection
```
User opens app → Generate/retrieve user ID → Connect to WebSocket with user identification
```

### 2. Call Association
```
User starts recording → Generate/get call SID → Associate call with user ID via WebSocket & HTTP
```

### 3. Fraud Detection
```
Transcript received → Fraud detected → Look up user ID for call → Send alert to specific user only
```

### 4. Notification Delivery
```
Fraud alert → Targeted to specific user → Modal displayed → User can hang up or continue
```

## API Endpoints

### WebSocket Endpoints

#### Connect with User Identification
```
GET /api/ws?user_id={userId}&phone_number={phoneNumber}
```

**Query Parameters:**
- `user_id`: Unique user identifier
- `phone_number`: User's phone number

### HTTP Endpoints

#### Associate Call with User
```
POST /api/associate-call
Content-Type: application/json

{
  "call_sid": "CA1234567890",
  "user_id": "user_1234567890_1234567890", 
  "phone_number": "+1234567890"
}
```

#### Get Active Connections (Debug)
```
GET /api/active-connections
```

**Response:**
```json
{
  "total_connections": 2,
  "connections": [
    {
      "user_id": "user_1234567890_1234567890",
      "phone_number": "+1234567890"
    }
  ],
  "call_mappings": {
    "CA1234567890": "user_1234567890_1234567890"
  }
}
```

## Implementation Details

### User ID Management

**Generation Strategy:**
1. Check AsyncStorage for existing user ID
2. If not found, generate: `user_{phoneHash}_{timestamp}`
3. Store in AsyncStorage for persistence
4. Use across app sessions

**Phone Number Processing:**
```typescript
const phoneHash = userPhoneNumber.replace(/[^0-9]/g, '');
```

### Call Association Methods

**1. WebSocket Association (Primary):**
- Immediate association via WebSocket message
- Real-time confirmation from server
- Handles connection state changes

**2. HTTP Association (Backup):**
- HTTP POST request for reliability
- Fallback if WebSocket fails
- Persistent server-side storage

### Fraud Alert Targeting

**Targeting Logic:**
1. Get call SID from fraud detection trigger
2. Look up user ID in `call_to_user_mapping`
3. Find user's WebSocket connection in `active_connections`
4. Send fraud alert only to that specific connection
5. Fallback to broadcast if mapping not found

### Error Handling

**Connection Failures:**
- Automatic reconnection attempts
- Graceful degradation to broadcast mode
- User feedback for connection issues

**Association Failures:**
- Dual-method association (WebSocket + HTTP)
- Retry mechanisms
- Fallback to broadcast notifications

## Testing

### Debug Component Features

**Connection Status:**
- User ID display
- Phone number verification
- WebSocket connection state
- Call SID tracking

**Server Information:**
- Total active connections
- User connection details
- Call-to-user mappings
- Real-time updates

**Test Controls:**
- Manual call association testing
- Fraud alert simulation
- Debug information refresh
- Connection status verification

### Testing Scenarios

1. **Single User Test:**
   - One user connects
   - Starts recording
   - Associates call
   - Receives targeted fraud alert

2. **Multiple User Test:**
   - Multiple users connect
   - Each starts recording different calls
   - Each receives only their own fraud alerts

3. **Connection Recovery:**
   - User disconnects and reconnects
   - Call association persists
   - Fraud alerts continue to work

## Benefits

### 1. Targeted Notifications
- Users only receive relevant fraud alerts
- Eliminates notification spam
- Improves user experience

### 2. Scalability
- Efficient resource usage
- Supports multiple concurrent users
- Minimal server overhead

### 3. Reliability
- Multiple association methods
- Fallback mechanisms
- Persistent user identification

### 4. Debugging
- Comprehensive debug tools
- Real-time monitoring
- Easy troubleshooting

## Configuration

### Backend Configuration
```python
# WebSocket endpoint with user identification
@router.websocket("/ws")
async def websocket_endpoint(
    websocket: WebSocket, 
    user_id: str = Query(...),
    phone_number: str = Query(...)
):
```

### Frontend Configuration
```typescript
const {isFraud, setIsFraud, ws, userId, isConnected, associateCall} = useWebSocket({
  url: 'wss://your-backend.com/api/ws',
  userPhoneNumber: userPhoneNumber
});
```

## Deployment Notes

1. **Environment Variables:**
   - Update WebSocket URLs for production
   - Configure CORS settings
   - Set up SSL certificates

2. **Database Integration:**
   - Consider persistent storage for user mappings
   - Implement user session management
   - Add call history tracking

3. **Monitoring:**
   - Add connection metrics
   - Monitor fraud detection accuracy
   - Track user engagement

## Future Enhancements

1. **User Authentication:**
   - Implement proper user accounts
   - Add authentication tokens
   - Secure WebSocket connections

2. **Advanced Targeting:**
   - Group notifications
   - Priority levels
   - Custom notification preferences

3. **Analytics:**
   - Fraud detection statistics
   - User behavior analysis
   - Performance metrics

4. **Mobile Optimizations:**
   - Background connection handling
   - Battery usage optimization
   - Network resilience improvements
