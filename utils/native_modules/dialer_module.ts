import { NativeModules } from 'react-native';

const { DialerModule } = NativeModules;

export const make_call = (number: string) => {
  DialerModule.dialNumber(number);
};
