import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:provider/provider.dart';

import '../auth_provider.dart';
import '../models.dart';
import 'status_chip.dart';

const _settable = ['PENDING', 'PRINTING', 'READY', 'COLLECTED', 'REJECTED'];

class AdminHome extends StatefulWidget {
  const AdminHome({super.key});
  @override
  State<AdminHome> createState() => _AdminHomeState();
}

class _AdminHomeState extends State<AdminHome> {
  late Future<List<PrintJob>> _jobs;
  bool _activeOnly = true;

  @override
  void initState() {
    super.initState();
    _jobs = _load();
  }

  Future<List<PrintJob>> _load() => context.read<AuthProvider>().api.allJobs();

  Future<void> _refresh() async {
    setState(() => _jobs = _load());
    await _jobs;
  }

  Future<void> _setStatus(PrintJob job, String status) async {
    try {
      await context.read<AuthProvider>().api.setStatus(job.id, status);
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Job ${job.id} marked as ${statusLabels[status]!.toLowerCase()}.')));
      _refresh();
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(e.toString())));
    }
  }

  @override
  Widget build(BuildContext context) {
    final auth = context.watch<AuthProvider>();
    return Scaffold(
      appBar: AppBar(
        title: const Text('Print desk'),
        actions: [IconButton(tooltip: 'Sign out', icon: const Icon(Icons.logout), onPressed: auth.logout)],
      ),
      body: RefreshIndicator(
        onRefresh: _refresh,
        child: FutureBuilder<List<PrintJob>>(
          future: _jobs,
          builder: (context, snap) {
            if (snap.connectionState != ConnectionState.done) return const Center(child: CircularProgressIndicator());
            if (snap.hasError) return ListView(padding: const EdgeInsets.all(24), children: [Text('${snap.error}')]);

            final all = snap.data!;
            final shown = _activeOnly ? all.where((j) => const ['PENDING', 'PRINTING', 'READY'].contains(j.status)).toList() : all;
            int count(String s) => all.where((j) => j.status == s).length;

            return ListView(
              padding: const EdgeInsets.all(16),
              children: [
                Row(
                  children: [
                    _Stat(count('PENDING'), 'Waiting'),
                    _Stat(count('PRINTING'), 'Printing'),
                    _Stat(count('READY'), 'Ready'),
                  ],
                ),
                const SizedBox(height: 12),
                Wrap(
                  spacing: 8,
                  children: [
                    ChoiceChip(label: const Text('To do'), selected: _activeOnly, onSelected: (_) => setState(() => _activeOnly = true)),
                    ChoiceChip(label: const Text('All jobs'), selected: !_activeOnly, onSelected: (_) => setState(() => _activeOnly = false)),
                  ],
                ),
                const SizedBox(height: 8),
                if (shown.isEmpty)
                  const Padding(padding: EdgeInsets.all(32), child: Center(child: Text('Nothing here. Jobs will appear in this list.')))
                else
                  for (final j in shown) _AdminJobCard(job: j, onStatus: (s) => _setStatus(j, s)),
              ],
            );
          },
        ),
      ),
    );
  }
}

class _Stat extends StatelessWidget {
  final int n;
  final String label;
  const _Stat(this.n, this.label);

  @override
  Widget build(BuildContext context) => Expanded(
        child: Container(
          margin: const EdgeInsets.only(right: 8),
          padding: const EdgeInsets.all(14),
          decoration: BoxDecoration(color: Colors.white, borderRadius: BorderRadius.circular(10), border: Border.all(color: const Color(0xFFD6DEE5))),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text('$n', style: const TextStyle(fontSize: 26, fontWeight: FontWeight.w700)),
              Text(label, style: const TextStyle(fontSize: 12, color: Colors.black54)),
            ],
          ),
        ),
      );
}

class _AdminJobCard extends StatelessWidget {
  final PrintJob job;
  final void Function(String status) onStatus;
  const _AdminJobCard({required this.job, required this.onStatus});

  @override
  Widget build(BuildContext context) {
    return Card(
      elevation: 0,
      color: Colors.white,
      margin: const EdgeInsets.only(top: 10),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10), side: const BorderSide(color: Color(0xFFD6DEE5))),
      child: Padding(
        padding: const EdgeInsets.all(14),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(job.fileName, maxLines: 1, overflow: TextOverflow.ellipsis, style: const TextStyle(fontWeight: FontWeight.w700)),
            Text('${job.studentName}${job.studentRoll != null ? ' · Roll ${job.studentRoll}' : ''}'),
            Text('${job.serviceName} · ${job.pages} × ${job.copies} · ₹${job.totalPrice.toStringAsFixed(2)}'),
            if (job.notes.isNotEmpty) Text('“${job.notes}”', style: const TextStyle(fontStyle: FontStyle.italic, color: Colors.black54)),
            Text('Job ${job.id} · ${DateFormat('d MMM, h:mm a').format(job.createdAt)}', style: const TextStyle(fontSize: 12, color: Colors.black54)),
            const SizedBox(height: 10),
            if (job.status == 'CANCELLED')
              const StatusChip('CANCELLED')
            else
              DropdownButtonFormField<String>(
                value: job.status,
                decoration: const InputDecoration(labelText: 'Status', isDense: true),
                items: [for (final s in _settable) DropdownMenuItem(value: s, child: Text(statusLabels[s]!))],
                onChanged: (s) {
                  if (s != null && s != job.status) onStatus(s);
                },
              ),
          ],
        ),
      ),
    );
  }
}
