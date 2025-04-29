declare module 'react-native-contacts' {
  interface PhoneNumber {
    number: string;
    label?: string;
  }

  interface Contact {
    recordID: string;
    displayName: string;
    phoneNumbers?: PhoneNumber[];
    givenName?: string;
    familyName?: string;
    emailAddresses?: string[];
  }

  export const getAll: () => Promise<Contact[]>;
  export const requestPermission: () => Promise<
    'authorized' | 'denied' | 'restricted'
  >;
}
