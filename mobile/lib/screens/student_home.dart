import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:provider/provider.dart';

import '../auth_provider.dart';
import '../models.dart';
import 'new_request_screen.dart';
import 'status_chip.dart';

class StudentHome extends StatefulWidget {
  const StudentHome({super.key});
  @override
  State<StudentHome> createState() => _StudentHomeState();
}

class _StudentHomeState extends State<StudentHome> {
  late Future<List<PrintJob>> _jobs;

  @override
  void initState() {
    super.initState();
    _jobs = _load();
  }

  Future<List<PrintJob>> _load() => context.read<AuthProvider>().api.myJobs();

  Future<void> _refresh() async {
    setState(() => _jobs = _load());
    await _jobs;
  }

  Future<void> _cancel(PrintJob job) async {
    try {
      await context.read<AuthProvider>().api.cancelJob(job.id);
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Print job cancelled.')));
      _refresh();
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(e.toString())));
    }
  }

  Future<void> _newJob() async {
    final sent = await Navigator.push<bool>(context, MaterialPageRoute(builder: (_) => const NewRequestScreen()));
    if (sent == true) _refresh();
  }

  @override
  Widget build(BuildContext context) {
    final auth = context.watch<AuthProvider>();
    return Scaffold(
      appBar: AppBar(
        title: Text('Hello, ${auth.user!.name.split(' ').first}'),
        actions: [IconButton(tooltip: 'Sign out', icon: const Icon(Icons.logout), onPressed: auth.logout)],
      ),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: _newJob,
        icon: const Icon(Icons.upload_file),
        label: const Text('New print job'),
      ),
      body: RefreshIndicator(
        onRefresh: _refresh,
        child: FutureBuilder<List<PrintJob>>(
          future: _jobs,
          builder: (context, snap) {
            if (snap.connectionState != ConnectionState.done) {
              return const Center(child: CircularProgressIndicator());
            }
            if (snap.hasError) {
              return ListView(padding: const EdgeInsets.all(24), children: [Text('${snap.error}')]);
            }
            final jobs = snap.data!;
            if (jobs.isEmpty) {
              return ListView(
                padding: const EdgeInsets.all(32),
                children: const [
                  SizedBox(height: 80),
                  Icon(Icons.print_outlined, size: 48, color: Color(0xFF0B7A97)),
                  SizedBox(height: 12),
                  Text('No jobs yet', textAlign: TextAlign.center, style: TextStyle(fontWeight: FontWeight.w700, fontSize: 18)),
                  SizedBox(height: 4),
                  Text('Tap “New print job” to send your first file.', textAlign: TextAlign.center),
                ],
              );
            }
            return ListView.separated(
              padding: const EdgeInsets.fromLTRB(16, 16, 16, 96),
              itemCount: jobs.length,
              separatorBuilder: (_, __) => const SizedBox(height: 10),
              itemBuilder: (_, i) => _JobCard(job: jobs[i], onCancel: () => _cancel(jobs[i])),
            );
          },
        ),
      ),
    );
  }
}

class _JobCard extends StatelessWidget {
  final PrintJob job;
  final VoidCallback onCancel;
  const _JobCard({required this.job, required this.onCancel});

  @override
  Widget build(BuildContext context) {
    return Card(
      elevation: 0,
      color: Colors.white,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10), side: const BorderSide(color: Color(0xFFD6DEE5))),
      child: Padding(
        padding: const EdgeInsets.all(14),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Expanded(child: Text(job.fileName, maxLines: 1, overflow: TextOverflow.ellipsis, style: const TextStyle(fontWeight: FontWeight.w700))),
                StatusChip(job.status),
              ],
            ),
            const SizedBox(height: 4),
            Text('${job.serviceName} · ${job.pages} × ${job.copies} · ₹${job.totalPrice.toStringAsFixed(2)}'),
            Text('Job ${job.id} · ${DateFormat('d MMM, h:mm a').format(job.createdAt)}', style: const TextStyle(fontSize: 12, color: Colors.black54)),
            if (job.status == 'READY') ...[
              const SizedBox(height: 8),
              Text('Ready at the desk. Pay ₹${job.totalPrice.toStringAsFixed(2)}.', style: const TextStyle(fontWeight: FontWeight.w600, color: Color(0xFF085E75))),
            ],
            if (job.status == 'PENDING')
              Align(
                alignment: Alignment.centerRight,
                child: TextButton(onPressed: onCancel, child: const Text('Cancel job', style: TextStyle(color: Color(0xFFB3205A)))),
              ),
          ],
        ),
      ),
    );
  }
}
