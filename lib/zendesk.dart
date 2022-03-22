import 'package:flutter/services.dart';

class Zendesk {
  Zendesk._();
  static Zendesk instance = Zendesk._();

  final MethodChannel channel = const MethodChannel('zendesk2');

  bool _chatInitialized = false;

  /// Initialize the Zendesk Chat SDK
  ///
  /// ```accountKey``` the zendesk created account key, unique by organization
  ///
  /// ```appId``` the app ID created on Zendesk Panel
  Future<void> initChatSDK(
    String accountKey,
    String appId,
  ) async {
    if (_chatInitialized) return;
    Map arguments = {
      'accountKey': accountKey,
      'appId': appId,
    };
    try {
      await channel.invokeMethod('init_chat', arguments);
      _chatInitialized = true;
    } catch (e) {
      print(e);
    }
  }
}
