/// Base URL of the Spring Boot API.
/// Android emulator: 10.0.2.2 is the emulator's name for your computer's localhost.
/// Real deployment: pass your Render URL when building:
///   flutter run --dart-define=API_URL=https://campusprint-api.onrender.com
const String apiBaseUrl = String.fromEnvironment(
  'API_URL',
  defaultValue: 'http://10.0.2.2:8080',
);
