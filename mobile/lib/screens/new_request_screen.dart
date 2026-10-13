import 'package:file_picker/file_picker.dart';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../auth_provider.dart';
import '../models.dart';

class NewRequestScreen extends StatefulWidget {
  const NewRequestScreen({super.key});
  @override
  State<NewRequestScreen> createState() => _NewRequestScreenState();
}

class _NewRequestScreenState extends State<NewRequestScreen> {
  List<PrintOption> _options = [];
  PrintOption? _selected;
  PlatformFile? _file;
  final _pages = TextEditingController(text: '1');
  final _copies = TextEditingController(text: '1');
  final _notes = TextEditingController();
  String? _error;
  bool _loading = true;
  bool _busy = false;

  @override
  void initState() {
    super.initState();
    _loadOptions();
  }

  Future<void> _loadOptions() async {
    try {
      final list = await context.read<AuthProvider>().api.services();
      setState(() {
        _options = list;
        _selected = list.isEmpty ? null : list.first;
      });
    } catch (e) {
      setState(() => _error = e.toString());
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  int get _pageCount => int.tryParse(_pages.text) ?? 0;
  int get _copyCount => int.tryParse(_copies.text) ?? 0;
  // Preview only. The server calculates the real total.
  double get _total => (_selected?.pricePerPage ?? 0) * _pageCount * _copyCount;

  Future<void> _pick() async {
    final result = await FilePicker.platform.pickFiles(
      type: FileType.custom,
      allowedExtensions: ['pdf', 'doc', 'docx', 'ppt', 'pptx', 'jpg', 'jpeg', 'png', 'txt'],
    );
    if (result == null) return;
    final f = result.files.single;
    if (f.size > 10 * 1024 * 1024) {
      setState(() => _error = 'That file is larger than 10 MB.');
      return;
    }
    setState(() { _file = f; _error = null; });
  }

  Future<void> _submit() async {
    if (_file?.path == null) { setState(() => _error = 'Attach the file you want printed.'); return; }
    if (_selected == null) { setState(() => _error = 'Choose a print option.'); return; }
    if (_pageCount < 1 || _copyCount < 1) { setState(() => _error = 'Pages and copies must be at least 1.'); return; }
    setState(() { _busy = true; _error = null; });
    try {
      await context.read<AuthProvider>().api.createJob(
            filePath: _file!.path!,
            serviceId: _selected!.id,
            pages: _pageCount,
            copies: _copyCount,
            notes: _notes.text.trim(),
          );
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Sent to the print desk.')));
      Navigator.pop(context, true);
    } catch (e) {
      setState(() => _error = e.toString());
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  @override
  void dispose() {
    _pages.dispose();
    _copies.dispose();
    _notes.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('New print job')),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : SingleChildScrollView(
              padding: const EdgeInsets.all(20),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  OutlinedButton.icon(
                    onPressed: _pick,
                    icon: Icon(_file == null ? Icons.upload_file : Icons.description),
                    label: Text(_file?.name ?? 'Choose a file (PDF, Word, image…)', overflow: TextOverflow.ellipsis),
                    style: OutlinedButton.styleFrom(minimumSize: const Size.fromHeight(56), backgroundColor: Colors.white),
                  ),
                  const SizedBox(height: 16),
                  DropdownButtonFormField<PrintOption>(
                    value: _selected,
                    isExpanded: true,
                    decoration: const InputDecoration(labelText: 'Print option'),
                    items: [
                      for (final o in _options)
                        DropdownMenuItem(value: o, child: Text('${o.name} · ₹${o.pricePerPage}/page', overflow: TextOverflow.ellipsis)),
                    ],
                    onChanged: (v) => setState(() => _selected = v),
                  ),
                  const SizedBox(height: 14),
                  Row(
                    children: [
                      Expanded(
                        child: TextField(
                          controller: _pages,
                          keyboardType: TextInputType.number,
                          decoration: const InputDecoration(labelText: 'Pages in the file'),
                          onChanged: (_) => setState(() {}),
                        ),
                      ),
                      const SizedBox(width: 12),
                      Expanded(
                        child: TextField(
                          controller: _copies,
                          keyboardType: TextInputType.number,
                          decoration: const InputDecoration(labelText: 'Copies'),
                          onChanged: (_) => setState(() {}),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 14),
                  TextField(
                    controller: _notes,
                    maxLength: 300,
                    maxLines: 2,
                    decoration: const InputDecoration(labelText: 'Note for the desk (optional)'),
                  ),
                  const SizedBox(height: 8),
                  Container(
                    padding: const EdgeInsets.all(16),
                    decoration: BoxDecoration(color: Colors.white, borderRadius: BorderRadius.circular(8), border: Border.all(color: const Color(0xFFD6DEE5))),
                    child: Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        const Text('Total to pay at the desk'),
                        Text('₹${_total.toStringAsFixed(2)}', style: const TextStyle(fontSize: 26, fontWeight: FontWeight.w700, color: Color(0xFF085E75))),
                      ],
                    ),
                  ),
                  if (_error != null) ...[
                    const SizedBox(height: 12),
                    Text(_error!, style: const TextStyle(color: Color(0xFFB3205A))),
                  ],
                  const SizedBox(height: 20),
                  FilledButton(onPressed: _busy ? null : _submit, child: Text(_busy ? 'Sending…' : 'Send to print desk')),
                ],
              ),
            ),
    );
  }
}
