import Flutter
import UIKit
import ChatProvidersSDK

public class SwiftZendesk2Plugin: NSObject, FlutterPlugin {
    
    
    var chatStateObservationToken: ObservationToken? = nil
    var accountObservationToken: ObservationToken? = nil
    var settingsObservationToken: ObservationToken? = nil
    var statusObservationToken: ObservationToken? = nil
    
    private var streamingChatSDK: Bool = false
    
    private var channel: FlutterMethodChannel
    
    public static func register(with registrar: FlutterPluginRegistrar) -> Void {
        let channel = FlutterMethodChannel(name: "zendesk2", binaryMessenger: registrar.messenger())
        
        let instance = SwiftZendesk2Plugin(channel: channel)
        registrar.addMethodCallDelegate(instance, channel: channel)
        registrar.addApplicationDelegate(instance)
    }
    
    init(channel: FlutterMethodChannel) {
        self.channel = channel
    }
    
    public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) -> Void {
        let method = call.method
        let arguments = call.arguments as? Dictionary<String, Any>
        
        var mResult: Any? = nil
        
        let zendesk2Chat = SwiftZendesk2Chat(channel: channel, flutterPlugin: self)
        
        switch(method){
        // chat sdk method channels
        case "init_chat":
            zendesk2Chat.initialize(arguments)
            break;
        case "setVisitorInfo":
            zendesk2Chat.setVisitorInfo(arguments)
            break
        case "startChatProviders":
            if streamingChatSDK {
                NSLog("Chat Providers already started!")
            } else {
                zendesk2Chat.startChatProviders()
                streamingChatSDK = true
            }
            break
        case "sendChatProvidersResult":
            mResult = arguments
            break
        case "sendChatConnectionStatusResult":
            mResult = arguments
            break
        case "sendChatSettingsResult":
            mResult = arguments
            break
        case "sendChatIsOnlineResult":
            mResult = arguments
            break
        case "sendMessage":
            zendesk2Chat.sendMessage(arguments)
            break
        case "sendFile":
            zendesk2Chat.sendFile(arguments)
            break
        case "endChat":
            zendesk2Chat.endChat()
            break
        case "sendIsTyping":
            zendesk2Chat.sendTyping(arguments)
            break
        case "chat_connect":
            zendesk2Chat.connect()
            break
        case "chat_disconnect":
            zendesk2Chat.disconnect()
            break
        case "chat_dispose":
            self.chatStateObservationToken?.cancel()
            self.accountObservationToken?.cancel()
            self.settingsObservationToken?.cancel()
            self.statusObservationToken?.cancel()
            zendesk2Chat.dispose()
            streamingChatSDK = false
            break
        case "setVisitorNote":
            zendesk2Chat.setVisitorNote(arguments)
        case "sendEmailTranscript":
            zendesk2Chat.sendEmailTranscript(arguments)
        case "sendOfflineForm":
            zendesk2Chat.sendOfflineForm(arguments)
        case "enableLogger":
            zendesk2Chat.enableLogger()
        default:
            break
        }
        if mResult != nil {
            result(mResult)
        }
        result(nil)
    }
    
    
    public func application(_ application: UIApplication, didReceiveRemoteNotification userInfo: [AnyHashable : Any], fetchCompletionHandler completionHandler: @escaping (UIBackgroundFetchResult) -> Void) -> Bool {
        
        if Chat.pushNotificationsProvider?.isChatPushNotification(userInfo) ?? false {
            let application = UIApplication.shared
            Chat.didReceiveRemoteNotification(userInfo, in: application)
            completionHandler(.noData)
            return true
        }
        return false
    }
    
    public func userNotificationCenter(_ center: UNUserNotificationCenter,
                                       willPresent notification: UNNotification,
                                       withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void) {
        if Chat.pushNotificationsProvider?.isChatPushNotification(notification.request.content.userInfo) ?? false {
            completionHandler([.alert, .sound, .badge])
        }
    }
}
