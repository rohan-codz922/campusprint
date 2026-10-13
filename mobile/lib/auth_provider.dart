import 'package:flutter/foundation.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'api_service.dart';
import 'models.dart';

/// Holds "who is signed in" for the whole app. Widgets listen to it and rebuild when it changes.
class AuthProvider extends ChangeNotifier {
  final ApiService api = ApiService();
  AppUser? user;
  bool ready = false; // false while we check for a saved login on start-up

  static const _tokenKey = 'cp_token';

  /// Called once at start-up: if a token is saved, ask the server who it belongs to.
  Future<void> restore() async {
    final prefs = await SharedPreferences.getInstance();
    final saved = prefs.getString(_tokenKey);
    if (saved != null) {
      api.token = saved;
      try {
        user = await api.me();
      } catch (_) {
        api.token = null;
        await prefs.remove(_tokenKey);
      }
    }
    ready = true;
    notifyListeners();
  }

  Future<void> _startSession(Map<String, dynamic> data) async {
    api.token = data['token'] as String;
    user = AppUser.fromJson(data['user'] as Map<String, dynamic>);
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_tokenKey, api.token!);
    notifyListeners();
  }

  Future<void> login(String email, String password) async =>
      _startSession(await api.login(email.trim(), password));

  Future<void> register(String name, String rollNo, String email, String password) async =>
      _startSession(await api.register(name.trim(), rollNo.trim(), email.trim(), password));

  Future<void> logout() async {
    api.token = null;
    user = null;
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove(_tokenKey);
    notifyListeners();
  }
}
