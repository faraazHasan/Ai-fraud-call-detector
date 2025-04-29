import React from "react";
import {
	Pressable,
	ActivityIndicator,
	Text,
	StyleSheet,
	StyleProp,
	ViewStyle,
} from "react-native";

interface IProps {
	disabled?: boolean;
	onPress?: () => void;
	loading?: boolean;
	buttonText: string;
	style?: StyleProp<ViewStyle>;
	textStyle?: StyleProp<ViewStyle>;
	loadingColor?: string;
}
const CustomButton = (props: IProps) => {
	const {
		disabled,
		onPress,
		loading,
		buttonText,
		style = {},
		textStyle = {},
		loadingColor = "#fff",
	} = props;
	return (
		<Pressable
			style={StyleSheet.compose(
				disabled ? styles.authBtnDisabled : styles.themeButton,
				style
			)}
			onPress={onPress}
			disabled={disabled}
		>
			{loading ? (
				<ActivityIndicator size="small" color={loadingColor} />
			) : (
				<Text style={StyleSheet.compose(styles.btnText, textStyle)}>
					{buttonText}
				</Text>
			)}
		</Pressable>
	);
};

export default CustomButton;
const styles = StyleSheet.create({
	themeButton: {
		borderRadius: 10,
		alignItems: "center",
		padding: 15,
		marginTop: 20,
		fontFamily: "Poppins-Medium",
	},
	btnText: {
		fontSize: 12,
		textTransform: "capitalize",
		textAlign: "center",
		color: "#fff",
		fontFamily: "Poppins-Medium",
	},
	authBtnDisabled: {
		opacity: 0.4,
		borderRadius: 10,
		alignItems: "center",
		padding: 15,
		marginTop: 20,
		fontFamily: "Poppins-Medium",
	},
});
