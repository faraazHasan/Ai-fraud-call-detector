package com.spamcalldetector.activities.contacts;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.Arguments;
import android.content.ContentResolver;
import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.content.Context;
import android.net.Uri;
import android.content.ContentUris;
import java.util.ArrayList;
import androidx.core.content.ContextCompat;
import android.content.pm.PackageManager;
import android.Manifest;

public class ContactsModule extends ReactContextBaseJavaModule {

    private final ReactApplicationContext reactContext;

    public ContactsModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
    }

    @Override
    public String getName() {
        return "ContactsModule";
    }

    @ReactMethod
    public void checkPermissionAndFetchContacts(Callback successCallback, Callback errorCallback) {
        try {
            // Check if READ_CONTACTS permission is granted
            if (ContextCompat.checkSelfPermission(reactContext, Manifest.permission.READ_CONTACTS) 
                != PackageManager.PERMISSION_GRANTED) {
                errorCallback.invoke("READ_CONTACTS permission not granted. Please grant contacts permission to view contacts.");
                return;
            }
            
            ContentResolver contentResolver = reactContext.getContentResolver();
            Cursor cursor = contentResolver.query(
                    ContactsContract.Contacts.CONTENT_URI,
                    null,
                    null,
                    null,
                    null);

            if (cursor != null && cursor.getCount() > 0) {
                WritableArray contactsList = Arguments.createArray(); // Use WritableArray to store contacts

                while (cursor.moveToNext()) {
                    String contactName = cursor
                            .getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                    String contactId = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
                    String contactPhotoUri = cursor
                            .getString(cursor.getColumnIndex(ContactsContract.Contacts.PHOTO_URI));

                    // Prepare the phone numbers list
                    WritableArray phoneNumbers = Arguments.createArray(); // WritableArray to store phone numbers
                    if (cursor.getInt(cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)) > 0) {
                        Cursor phones = contentResolver.query(
                                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                null,
                                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                                new String[] { contactId },
                                null);
                        while (phones.moveToNext()) {
                            String phoneNumber = phones
                                    .getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                            phoneNumbers.pushString(phoneNumber); // Add phone numbers to WritableArray
                        }
                        phones.close();
                    }

                    // Prepare the email addresses list
                    WritableArray emails = Arguments.createArray();
                    Cursor emailCursor = contentResolver.query(
                            ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?",
                            new String[] { contactId },
                            null);
                    while (emailCursor != null && emailCursor.moveToNext()) {
                        String email = emailCursor
                                .getString(emailCursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS));
                        emails.pushString(email);
                    }
                    if (emailCursor != null) {
                        emailCursor.close();
                    }

                    // Prepare the address list
                    WritableArray addresses = Arguments.createArray();
                    Cursor addressCursor = contentResolver.query(
                            ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.StructuredPostal.CONTACT_ID + " = ?",
                            new String[] { contactId },
                            null);
                    while (addressCursor != null && addressCursor.moveToNext()) {
                        String address = addressCursor.getString(addressCursor
                                .getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS));
                        addresses.pushString(address);
                    }
                    if (addressCursor != null) {
                        addressCursor.close();
                    }

                    // Query organization and job title from ContactsContract.Data
                    String organization = "";
                    String jobTitle = "";
                    Cursor orgCursor = contentResolver.query(
                            ContactsContract.Data.CONTENT_URI,
                            null,
                            ContactsContract.Data.CONTACT_ID + " = ? AND " + ContactsContract.Data.MIMETYPE + " = ?",
                            new String[] { contactId, ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE },
                            null);
                    if (orgCursor != null && orgCursor.moveToFirst()) {
                        organization = orgCursor.getString(
                                orgCursor.getColumnIndex(ContactsContract.CommonDataKinds.Organization.COMPANY));
                        jobTitle = orgCursor.getString(
                                orgCursor.getColumnIndex(ContactsContract.CommonDataKinds.Organization.TITLE));
                        orgCursor.close();
                    }

                    // Create a WritableMap for each contact
                    WritableMap contact = Arguments.createMap();
                    contact.putString("contactId", contactId); // Add contact ID to map
                    contact.putString("contactName", contactName); // Add contact name to map
                    contact.putArray("phoneNumbers", phoneNumbers); // Add phone numbers to map
                    contact.putArray("emails", emails); // Add emails to map
                    contact.putArray("addresses", addresses); // Add addresses to map
                    contact.putString("organization", organization); // Add organization to map
                    contact.putString("jobTitle", jobTitle); // Add job title to map
                    contact.putString("photoUri", contactPhotoUri); // Add photo URI to map

                    // Add the contact map to the contacts list
                    contactsList.pushMap(contact);
                }
                cursor.close();

                // Return the contacts list via the success callback
                successCallback.invoke(contactsList);
            } else {
                errorCallback.invoke("No contacts found");
            }
        } catch (Exception e) {
            errorCallback.invoke(e.getMessage());
        }
    }

    @ReactMethod
    public void addContact(String name, String phoneNumber, Callback successCallback, Callback errorCallback) {
        try {
            // Check if WRITE_CONTACTS permission is granted
            if (ContextCompat.checkSelfPermission(reactContext, Manifest.permission.WRITE_CONTACTS) 
                != PackageManager.PERMISSION_GRANTED) {
                errorCallback.invoke("WRITE_CONTACTS permission not granted. Please grant contacts permission to add contacts.");
                return;
            }
            
            ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();

            // Add the contact name
            ops.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                    .build());

            // Add the display name
            ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(ContactsContract.Data.MIMETYPE,
                            ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name)
                    .build());

            // Add the phone number
            ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phoneNumber)
                    .withValue(ContactsContract.CommonDataKinds.Phone.TYPE,
                            ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                    .build());

            // Execute all the operations at once
            reactContext.getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);

            // Return success
            successCallback.invoke("Contact added successfully");
        } catch (Exception e) {
            errorCallback.invoke("Error adding contact: " + e.getMessage());
        }
    }

    @ReactMethod
    public void deleteContact(String contactId, Callback successCallback, Callback errorCallback) {
        try {
            // Check if WRITE_CONTACTS permission is granted
            if (ContextCompat.checkSelfPermission(reactContext, Manifest.permission.WRITE_CONTACTS) 
                != PackageManager.PERMISSION_GRANTED) {
                errorCallback.invoke("WRITE_CONTACTS permission not granted. Please grant contacts permission to delete contacts.");
                return;
            }
            
            ContentResolver contentResolver = reactContext.getContentResolver();

            // First, check if the contact exists
            Cursor cursor = contentResolver.query(
                    ContactsContract.Contacts.CONTENT_URI,
                    null,
                    ContactsContract.Contacts._ID + " = ?",
                    new String[] { contactId },
                    null);

            if (cursor != null && cursor.moveToFirst()) {
                cursor.close();

                // Get all raw contact IDs for this contact
                Cursor rawContactCursor = contentResolver.query(
                        ContactsContract.RawContacts.CONTENT_URI,
                        new String[] { ContactsContract.RawContacts._ID },
                        ContactsContract.RawContacts.CONTACT_ID + "=?",
                        new String[] { contactId },
                        null);

                // Delete each raw contact
                while (rawContactCursor != null && rawContactCursor.moveToNext()) {
                    String rawContactId = rawContactCursor.getString(
                            rawContactCursor.getColumnIndex(ContactsContract.RawContacts._ID));
                    Uri deleteUri = ContentUris.withAppendedId(
                            ContactsContract.RawContacts.CONTENT_URI,
                            Long.parseLong(rawContactId));
                    contentResolver.delete(deleteUri, null, null);
                }

                if (rawContactCursor != null) {
                    rawContactCursor.close();
                }

                // Return success
                successCallback.invoke("Contact deleted successfully");
            } else {
                if (cursor != null) {
                    cursor.close();
                }
                errorCallback.invoke("Contact not found");
            }
        } catch (Exception e) {
            errorCallback.invoke("Error deleting contact: " + e.getMessage());
        }
    }
}
