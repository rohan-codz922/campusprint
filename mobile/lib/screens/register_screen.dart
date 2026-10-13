import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../auth_provider.dart';

class RegisterScreen extends StatefulWidget {
  const RegisterScreen({super.key});
  @override
  State<RegisterScreen> createState() => _RegisterScreenState();
}

class _RegisterScreenState extends State<RegisterScreen> {
  final _name = TextEditingController();
  final _roll = TextEditingController();
  final _email = TextEditingController();
  final _password = TextEditingController();
  String? _error;
  bool _busy = false;

  Future<void> _submit() async {
    setState(() { _busy = true; _error = null; });
    try {
      await context.read<AuthProvider>().register(_name.text, _roll.text, _email.text, _password.text);
      if (mounted) Navigator.pop(context); // the root screen switches to the student home by itself
    } catch (e) {
      setState(() => _error = e.toString());
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  @override
  void dispose() {
    for (final c in [_name, _roll, _email, _password]) {
      c.dispose();
    }
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Create your account')),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(24),
          child: Column(
            children: [
              TextField(controller: _name, decoration: const InputDecoration(labelText: 'Full name')),
              const SizedBox(height: 14),
              TextField(controller: _roll, decoration: const InputDecoration(labelText: 'Roll number (optional)')),
              const SizedBox(height: 14),
              TextField(controller: _email, keyboardType: TextInputType.emailAddress, decoration: const InputDecoration(labelText: 'College email')),
              const SizedBox(height: 14),
              TextField(
                controller: _password,
                obscureText: true,
                decoration: const InputDecoration(labelText: 'Password', helperText: 'At least 6 characters'),
              ),
              if (_error != null) ...[
                const SizedBox(height: 12),
                Align(alignment: Alignment.centerLeft, child: Text(_error!, style: const TextStyle(color: Color(0xFFB3205A)))),
              ],
              const SizedBox(height: 20),
              FilledButton(onPressed: _busy ? null : _submit, child: Text(_busy ? 'Creating account…' : 'Create account')),
            ],
          ),
        ),
      ),
    );
  }
}
