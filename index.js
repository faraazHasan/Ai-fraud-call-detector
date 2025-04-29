/**
 * @format
 */

import {AppRegistry} from 'react-native';
import App from './App';
import IncomingCall from './components/IncomingCall';
import OutgoingCall from './components/OutgoingCall';
import {name as appName} from './app.json';

AppRegistry.registerComponent(appName, () => App);
AppRegistry.registerComponent('IncomingCall', () => IncomingCall);
AppRegistry.registerComponent('OutgoingCall', () => OutgoingCall);
