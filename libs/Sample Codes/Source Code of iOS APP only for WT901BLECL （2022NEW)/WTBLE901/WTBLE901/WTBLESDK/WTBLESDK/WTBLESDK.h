//
//  WTBLESDK.h
//  WTBLESDK
//
//  Created by wit-motion on 2019/1/23.
//  Copyright © 2019 wit-motion. All rights reserved.
//

#import <Foundation/Foundation.h>
#import "WTBLECallback.h"
#import "WTDataCenterCallback.h"

@protocol WTBLEControlProtocol <NSObject>

- (void)startScan;

- (void)cancelScan;

- (void)tryConnectPeripheral:(CBPeripheral *)peripheral;

- (void)cancelConnection;

- (void)tryReceiveDataAfterConnected;

- (void)writeData:(NSData *)data;

@end

@protocol WTBLEApplyProtocol <NSObject>

- (void)sendTempuratureCommand;

- (void)sendMagneticCommand;

- (void)sendBaromemtricPressureCommand;

- (void)sendPortCommand;

- (void)sendQuaternionCommand;

- (void)accelerometerCali;

- (void)accelerometerCaliL;

- (void)accelerometerCaliR;

- (void)magneticCali;

- (void)finishMagneticCali;

- (void)D0Cali:(WTPortMode)portMode;

- (void)D1Cali:(WTPortMode)portMode;

- (void)D2Cali:(WTPortMode)portMode;

- (void)D3Cali:(WTPortMode)portMode;
// 0x01:0.1Hz 0x02:0.5Hz 0x03:1Hz 0x04:2Hz 0x05:5Hz 0x06:10Hz(默认) 0x07:20Hz 0x08:50Hz 0x09:100Hz
- (void)velocityCali:(float)rate;

- (void)save;

- (void)resume;

- (void)changeBLEDeviceName:(NSString *)name;

@end

@interface WTBLEParam : NSObject

// set filtered peripheral name, can be nil
@property (nonatomic, copy) NSString *peripheralFilter;

@end

@interface WTBLESDK : NSObject<WTBLEControlProtocol, WTBLEApplyProtocol>

@property (nonatomic, strong, readonly) WTBLEParam *param;

@property (nonatomic, strong, readonly) WTBLECallback *bleCallback;

@property (nonatomic, strong, readonly) WTDataCenterCallback *dataCallback;

+ (WTBLESDK *)sharedInstance;

@end
