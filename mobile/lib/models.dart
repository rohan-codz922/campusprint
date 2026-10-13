/// Plain Dart classes that mirror the JSON returned by the API.

class AppUser {
  final int id;
  final String name;
  final String email;
  final String role; // STUDENT or ADMIN
  final String? rollNo;

  AppUser({required this.id, required this.name, required this.email, required this.role, this.rollNo});

  bool get isAdmin => role == 'ADMIN';

  factory AppUser.fromJson(Map<String, dynamic> j) => AppUser(
        id: j['id'] as int,
        name: j['name'] as String,
        email: j['email'] as String,
        role: j['role'] as String,
        rollNo: j['rollNo'] as String?,
      );
}

class PrintOption {
  final int id;
  final String name;
  final String description;
  final double pricePerPage;
  final bool active;

  PrintOption({required this.id, required this.name, required this.description, required this.pricePerPage, required this.active});

  factory PrintOption.fromJson(Map<String, dynamic> j) => PrintOption(
        id: j['id'] as int,
        name: j['name'] as String,
        description: (j['description'] ?? '') as String,
        pricePerPage: (j['pricePerPage'] as num).toDouble(),
        active: j['active'] as bool,
      );
}

class PrintJob {
  final int id;
  final String fileName;
  final int pages;
  final int copies;
  final String notes;
  final double totalPrice;
  final String status;
  final DateTime createdAt;
  final String serviceName;
  final String studentName;
  final String? studentRoll;

  PrintJob({
    required this.id,
    required this.fileName,
    required this.pages,
    required this.copies,
    required this.notes,
    required this.totalPrice,
    required this.status,
    required this.createdAt,
    required this.serviceName,
    required this.studentName,
    this.studentRoll,
  });

  factory PrintJob.fromJson(Map<String, dynamic> j) => PrintJob(
        id: j['id'] as int,
        fileName: j['fileName'] as String,
        pages: j['pages'] as int,
        copies: j['copies'] as int,
        notes: (j['notes'] ?? '') as String,
        totalPrice: (j['totalPrice'] as num).toDouble(),
        status: j['status'] as String,
        createdAt: DateTime.parse(j['createdAt'] as String).toLocal(),
        serviceName: j['service']['name'] as String,
        studentName: j['student']['name'] as String,
        studentRoll: j['student']['rollNo'] as String?,
      );
}

const statusLabels = {
  'PENDING': 'Submitted',
  'PRINTING': 'Printing',
  'READY': 'Ready to collect',
  'COLLECTED': 'Collected',
  'REJECTED': 'Rejected',
  'CANCELLED': 'Cancelled',
};
