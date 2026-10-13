import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import 'auth_provider.dart';
import 'screens/admin_home.dart';
import 'screens/login_screen.dart';
import 'screens/student_home.dart';

const cyan = Color(0xFF0B7A97);
const ink = Color(0xFF14202B);

void main() {
  runApp(
    ChangeNotifierProvider(
      create: (_) => AuthProvider()..restore(),
      child: const CampusPrintApp(),
    ),
  );
}

class CampusPrintApp extends StatelessWidget {
  const CampusPrintApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'CampusPrint',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        useMaterial3: true,
        colorScheme: ColorScheme.fromSeed(seedColor: cyan, primary: cyan),
        scaffoldBackgroundColor: const Color(0xFFF2F5F7),
        appBarTheme: const AppBarTheme(
          backgroundColor: Colors.white,
          foregroundColor: ink,
          elevation: 0,
          scrolledUnderElevation: 1,
          titleTextStyle: TextStyle(color: ink, fontSize: 20, fontWeight: FontWeight.w700),
        ),
        inputDecorationTheme: InputDecorationTheme(
          filled: true,
          fillColor: Colors.white,
          border: OutlineInputBorder(borderRadius: BorderRadius.circular(8)),
        ),
        filledButtonTheme: FilledButtonThemeData(
          style: FilledButton.styleFrom(
            minimumSize: const Size.fromHeight(50),
            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
          ),
        ),
      ),
      home: const _Root(),
    );
  }
}

/// Decides which screen to show from the signed-in user's role.
/// This is the mobile twin of the web app's ProtectedRoute.
class _Root extends StatelessWidget {
  const _Root();

  @override
  Widget build(BuildContext context) {
    final auth = context.watch<AuthProvider>();
    if (!auth.ready) {
      return const Scaffold(body: Center(child: CircularProgressIndicator()));
    }
    if (auth.user == null) return const LoginScreen();
    return auth.user!.isAdmin ? const AdminHome() : const StudentHome();
  }
}
