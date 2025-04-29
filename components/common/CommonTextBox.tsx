import { useState } from "react";
import {
  TextInput,
  StyleSheet,
  KeyboardTypeOptions,
  StyleProp,
  TextStyle,
  InputModeOptions,
} from "react-native";
import { Control, Controller } from "react-hook-form";
import CommonError from "./CommonError";
import React from "react";

interface IProps {
  placeHolder?: string | undefined;
  keyboardType?: KeyboardTypeOptions | undefined;
  type?: string;
  step?: string;
  className?: string;
  icon?: boolean;
  name: string;
  align?: string;
  value?: string;
  control?: Control<any>;
  inputIcon?: string;
  error?: any;
  parentClassName?: string;
  disabled?: boolean;
  readonly?: boolean;
  labelMandatory?: boolean;
  multiline?: boolean;
  numberOfLines?: number;
  rows?: number;
  label?: string;
  inputImg?: string;
  onIconClick?: () => void;
  passwordTogggle?: (event: boolean) => void;
  onClick?: () => void;
  inputMode?: InputModeOptions;
  secureTextEntry?: boolean;
  labelRequired?: boolean;
  style?: StyleProp<TextStyle> | undefined;
  placeholderTextColor?: string;
  onChangeText?: (text: string) => void;
}

const CustomTextInput: React.FC<IProps> = (props) => {
  const {
    placeHolder,
    value,
    name,
    control,
    style = styles.inputStyle,
    secureTextEntry,
    numberOfLines = undefined,
    multiline = false,
    inputMode = "text",
    readonly,
    placeholderTextColor = "#919AA2",
    onChangeText,
  } = props;

  const [isFocused, setIsFocused] = useState(false);

  const handleFocus = () => {
    setIsFocused(true);
  };

  const handleBlur = () => {
    setIsFocused(false);
  };

  return (
    <Controller
      render={({ field, fieldState }) => (
        <>
          <TextInput
            {...field}
            placeholder={placeHolder}
            keyboardType="phone-pad" // Enables numeric keypad for phone numbers
            // keyboardType={keyboardType}
            readOnly={readonly}
            style={[style, { borderColor: isFocused ? "#919AA2" : "#919AA2" }]}
            onFocus={handleFocus}
            secureTextEntry={secureTextEntry}
            onBlur={() => {
              handleBlur();
              field.onBlur();
            }}
            inputMode={inputMode}
            multiline={multiline}
            numberOfLines={numberOfLines}
            value={field.value}
            placeholderTextColor={placeholderTextColor}
            onChangeText={(text: string) => {
              field.onChange(text);
              if (onChangeText) {
                onChangeText(text); // Call the prop function if provided
              }
            }}
          />
          {fieldState?.error ? (
            <>
              <CommonError message={fieldState?.error?.message}></CommonError>
            </>
          ) : null}
        </>
      )}
      name={name}
      defaultValue={value || ""}
      control={control ? control : undefined}
    />
  );
};

const styles = StyleSheet.create({
  inputStyle: {
    borderColor: "#919AA2",
    borderWidth: 1,
    borderStyle: "solid",
    borderRadius: 10,
    color: "#000",
    paddingHorizontal: 15,
    height: 60,
    fontFamily: "Poppins-Regular",
    paddingTop: 18,
    paddingBottom: 10,
    fontSize: 12,
  },
});

export default CustomTextInput;
