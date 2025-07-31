package com.spamcalldetector.services

import android.net.Uri
import android.os.Bundle
import android.telecom.*
import android.util.Log
import com.spamcalldetector.helpers.Constants

/**
 * ConnectionService implementation required for default dialer app eligibility.
 * This service handles the creation of outgoing and incoming connections
 * as required by the Android Telecom framework.
 */
class CallConnectionService : ConnectionService() {
    
    companion object {
        private const val TAG = "CallConnectionService"
    }

    override fun onCreateOutgoingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?
    ): Connection? {
        Log.d(TAG, "onCreateOutgoingConnection called")
        
        if (request == null) {
            Log.e(TAG, "ConnectionRequest is null")
            return null
        }

        val connection = CallConnection(this, request.address, true)
        
        // Set connection properties as required by documentation
        connection.connectionProperties = Connection.PROPERTY_SELF_MANAGED
        
        // Set capabilities if app supports hold
        connection.connectionCapabilities = Connection.CAPABILITY_HOLD or Connection.CAPABILITY_SUPPORT_HOLD
        
        // Set caller display name
        val phoneNumber = request.address?.schemeSpecificPart ?: "Unknown"
        connection.setCallerDisplayName(phoneNumber, TelecomManager.PRESENTATION_ALLOWED)
        
        // Set video state from request
        connection.videoState = request.videoState
        
        Log.d(TAG, "Created outgoing connection for: $phoneNumber")
        return connection
    }

    override fun onCreateOutgoingConnectionFailed(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?
    ) {
        Log.e(TAG, "onCreateOutgoingConnectionFailed called")
        super.onCreateOutgoingConnectionFailed(connectionManagerPhoneAccount, request)
        
        // Inform user that outgoing call could not be placed
        // This could be due to emergency call in progress or other constraints
        Log.e(TAG, "Failed to create outgoing connection - call cannot be placed")
    }

    override fun onCreateIncomingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?
    ): Connection? {
        Log.d(TAG, "onCreateIncomingConnection called")
        
        if (request == null) {
            Log.e(TAG, "ConnectionRequest is null")
            return null
        }

        val connection = CallConnection(this, request.address, false)
        
        // Set connection properties as required by documentation
        connection.connectionProperties = Connection.PROPERTY_SELF_MANAGED
        
        // Set capabilities if app supports hold
        connection.connectionCapabilities = Connection.CAPABILITY_HOLD or Connection.CAPABILITY_SUPPORT_HOLD
        
        // Set caller display name
        val phoneNumber = request.address?.schemeSpecificPart ?: "Unknown"
        connection.setCallerDisplayName(phoneNumber, TelecomManager.PRESENTATION_ALLOWED)
        
        // Set address of incoming call
        connection.setAddress(request.address, TelecomManager.PRESENTATION_ALLOWED)
        
        // Set video state from request
        connection.videoState = request.videoState
        
        Log.d(TAG, "Created incoming connection from: $phoneNumber")
        return connection
    }

    override fun onCreateIncomingConnectionFailed(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?
    ) {
        Log.e(TAG, "onCreateIncomingConnectionFailed called")
        super.onCreateIncomingConnectionFailed(connectionManagerPhoneAccount, request)
        
        // Silently reject the incoming call and optionally post notification
        Log.e(TAG, "Failed to create incoming connection - call rejected")
    }
}

/**
 * Connection implementation that represents individual calls.
 * This class handles the call state and user interactions.
 */
class CallConnection(
    private val service: CallConnectionService,
    private val address: Uri?,
    private val isOutgoing: Boolean
) : Connection() {
    
    companion object {
        private const val TAG = "CallConnection"
    }
    
    private var isCallActive = false
    private var isCallOnHold = false

    init {
        Log.d(TAG, "CallConnection created - isOutgoing: $isOutgoing, address: $address")
        
        if (isOutgoing) {
            // For outgoing calls, set initial state
            setInitializing()
        } else {
            // For incoming calls, set ringing state
            setRinging()
        }
    }

    override fun onShowIncomingCallUi() {
        Log.d(TAG, "onShowIncomingCallUi called")
        // The telecom framework is requesting to show incoming call UI
        // This is handled by our InCallService (CallService)
    }

    override fun onCallAudioStateChanged(state: CallAudioState?) {
        Log.d(TAG, "onCallAudioStateChanged: ${state?.route}")
        super.onCallAudioStateChanged(state)
        // Handle audio route changes (speaker, bluetooth, etc.)
    }

    override fun onHold() {
        Log.d(TAG, "onHold called")
        super.onHold()
        
        if (isCallActive) {
            isCallOnHold = true
            isCallActive = false
            setOnHold()
            Log.d(TAG, "Call put on hold")
        }
    }

    override fun onUnhold() {
        Log.d(TAG, "onUnhold called")
        super.onUnhold()
        
        if (isCallOnHold) {
            isCallOnHold = false
            isCallActive = true
            setActive()
            Log.d(TAG, "Call resumed from hold")
        }
    }

    override fun onAnswer() {
        Log.d(TAG, "onAnswer called")
        super.onAnswer()
        
        if (!isOutgoing) {
            isCallActive = true
            setActive()
            Log.d(TAG, "Incoming call answered")
        }
    }

    override fun onAnswer(videoState: Int) {
        Log.d(TAG, "onAnswer called with videoState: $videoState")
        super.onAnswer(videoState)
        
        if (!isOutgoing) {
            isCallActive = true
            this.videoState = videoState
            setActive()
            Log.d(TAG, "Incoming call answered with video state: $videoState")
        }
    }

    override fun onReject() {
        Log.d(TAG, "onReject called")
        super.onReject()
        
        setDisconnected(DisconnectCause(DisconnectCause.REJECTED))
        destroy()
        Log.d(TAG, "Call rejected")
    }

    override fun onDisconnect() {
        Log.d(TAG, "onDisconnect called")
        super.onDisconnect()
        
        isCallActive = false
        isCallOnHold = false
        setDisconnected(DisconnectCause(DisconnectCause.LOCAL))
        destroy()
        Log.d(TAG, "Call disconnected")
    }

    /**
     * Call this method when an outgoing call is connected
     */
    fun setCallConnected() {
        if (isOutgoing && !isCallActive) {
            isCallActive = true
            setActive()
            Log.d(TAG, "Outgoing call connected")
        }
    }
}
