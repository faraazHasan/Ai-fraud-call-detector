import React from 'react';
import {Text, StyleSheet} from 'react-native';
interface CallDurationProps {
  callDuration: string | null;
}

const CallDuration: React.FC<CallDurationProps> = (
  Props: CallDurationProps,
) => {
  const {callDuration} = Props;
  if (callDuration) {
    return (
      <>
        <Text style={styles.timer}>{callDuration}</Text>
      </>
    );
  } else {
    return null;
  }
};
const styles = StyleSheet.create({
  timer: {
    textAlign: 'center',
    color: '#000',
    fontSize: 14,
    fontWeight: '500',
    marginBottom: 20,
  },
});
export default CallDuration;
