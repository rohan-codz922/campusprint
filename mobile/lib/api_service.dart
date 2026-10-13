import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'package:http/http.dart' as http;

import 'config.dart';
import 'models.dart';

/// Shown to the user as-is, so messages are written in plain language.
class ApiException implements Exception {
  final String message;
  ApiException(this.message);
  @override
  String toString() => message;
}

/// The only class that talks to the network. Screens never build HTTP requests themselves.
class ApiService {
  String? token; // sent as "Authorization: Bearer <token>" on every request

  Uri _uri(String path) => Uri.parse('$apiBaseUrl$path');

  Map<String, String> get _headers => {
        if (token != null) 'Authorization': 'Bearer $token',
      };

  Map<String, String> get _jsonHeaders => {..._headers, 'Content-Type': 'application/json'};

  Future<dynamic> _handle(Future<http.Response> Function() send) async {
    try {
      // 60 s because a free-tier server can take a while to wake up.
      final res = await send().timeout(const Duration(seconds: 60));
      final body = res.body.isEmpty ? null : jsonDecode(utf8.decode(res.bodyBytes));
      if (res.statusCode >= 400) {
        final msg = (body is Map && body['message'] != null) ? body['message'] as String : 'Request failed (${res.statusCode})';
        throw ApiException(msg);
      }
      return body;
    } on SocketException {
      throw ApiException('Cannot reach the server. Check your connection and try again.');
    } on TimeoutException {
      throw ApiException('The server took too long to answer. Try again in a moment.');
    }
  }

  // ---- Auth ----
  Future<Map<String, dynamic>> login(String email, String password) async =>
      await _handle(() => http.post(_uri('/api/auth/login'),
          headers: _jsonHeaders, body: jsonEncode({'email': email, 'password': password}))) as Map<String, dynamic>;

  Future<Map<String, dynamic>> register(String name, String rollNo, String email, String password) async =>
      await _handle(() => http.post(_uri('/api/auth/register'),
          headers: _jsonHeaders,
          body: jsonEncode({'name': name, 'rollNo': rollNo.isEmpty ? null : rollNo, 'email': email, 'password': password}))) as Map<String, dynamic>;

  Future<AppUser> me() async =>
      AppUser.fromJson(await _handle(() => http.get(_uri('/api/auth/me'), headers: _headers)) as Map<String, dynamic>);

  // ---- Price list ----
  Future<List<PrintOption>> services() async {
    final list = await _handle(() => http.get(_uri('/api/services'))) as List<dynamic>;
    return list.map((e) => PrintOption.fromJson(e as Map<String, dynamic>)).where((s) => s.active).toList();
  }

  // ---- Print jobs ----
  Future<List<PrintJob>> myJobs() async {
    final list = await _handle(() => http.get(_uri('/api/requests/mine'), headers: _headers)) as List<dynamic>;
    return list.map((e) => PrintJob.fromJson(e as Map<String, dynamic>)).toList();
  }

  Future<List<PrintJob>> allJobs() async {
    final list = await _handle(() => http.get(_uri('/api/requests'), headers: _headers)) as List<dynamic>;
    return list.map((e) => PrintJob.fromJson(e as Map<String, dynamic>)).toList();
  }

  /// Uploads the file as multipart/form-data together with the job details.
  Future<void> createJob({
    required String filePath,
    required int serviceId,
    required int pages,
    required int copies,
    required String notes,
  }) async {
    await _handle(() async {
      final req = http.MultipartRequest('POST', _uri('/api/requests'))
        ..headers.addAll(_headers)
        ..fields['serviceId'] = '$serviceId'
        ..fields['pages'] = '$pages'
        ..fields['copies'] = '$copies'
        ..fields['notes'] = notes
        ..files.add(await http.MultipartFile.fromPath('file', filePath));
      return http.Response.fromStream(await req.send());
    });
  }

  Future<void> cancelJob(int id) async =>
      await _handle(() => http.patch(_uri('/api/requests/$id/cancel'), headers: _headers));

  Future<void> setStatus(int id, String status) async => await _handle(() =>
      http.patch(_uri('/api/requests/$id/status'), headers: _jsonHeaders, body: jsonEncode({'status': status})));
}
