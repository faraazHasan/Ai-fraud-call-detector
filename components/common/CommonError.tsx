
import React from "react";
import { StyleSheet, Text, View } from "react-native";

interface ErrorProps {
	message?: string;
}
const CommonError: React.FC<ErrorProps> = ({ message }) => {
	return (
		<View>
			<Text style={styles.error}>{message}</Text>
		</View>
	);
};

const styles = StyleSheet.create({
	error: {
		color: "#ff0000",
		fontSize: 10,
		lineHeight: 14,
		fontFamily: "Poppins-Regular",
		marginTop: 4
	},
});
export default CommonError;
