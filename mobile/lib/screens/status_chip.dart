import 'package:flutter/material.dart';

import '../models.dart';

/// Colour + text, so the status never depends on colour alone.
class StatusChip extends StatelessWidget {
  final String status;
  const StatusChip(this.status, {super.key});

  @override
  Widget build(BuildContext context) {
    final (bg, fg) = switch (status) {
      'PENDING' => (const Color(0xFFFFF1C6), const Color(0xFF6B4E00)),
      'PRINTING' => (const Color(0xFFDFF1F6), const Color(0xFF085E75)),
      'READY' => (const Color(0xFF0B7A97), Colors.white),
      'COLLECTED' => (const Color(0xFFE7ECF0), const Color(0xFF44525F)),
      _ => (const Color(0xFFFBE5EE), const Color(0xFFB3205A)), // rejected / cancelled
    };
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
      decoration: BoxDecoration(color: bg, borderRadius: BorderRadius.circular(99)),
      child: Text(statusLabels[status] ?? status, style: TextStyle(color: fg, fontSize: 12, fontWeight: FontWeight.w600)),
    );
  }
}
