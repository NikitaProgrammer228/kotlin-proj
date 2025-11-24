//
//  WTBLESDK.m
//  WTBLESDK
//
//  Created by wit-motion on 2019/1/23.
//  Copyright © 2019 wit-motion. All rights reserved.
//

#import "WTBLESDK.h"
#import "WTBLECentralManager.h"
#import "WTDataCenterManager.h"

@implementation WTBLEParam

@end

@interface WTBLESDK()

@property (nonatomic, strong) WTBLECentralManager *bleManager;
@property (nonatomic, strong) WTDataCenterManager *dataManager;
@property (nonatomic, strong) WTBLECallback *bleCallback;
@property (nonatomic, strong) WTDataCenterCallback *dataCallback;
@property (nonatomic, strong) WTBLEParam *param;

@end

@implementation WTBLESDK

static dispatch_once_t _onceToken;
static WTBLESDK *_bleSDK = nil;

+ (WTBLESDK * _Nonnull)sharedInstance{
    dispatch_once(&_onceToken, ^{
        _bleSDK = [[WTBLESDK alloc] init];
        _bleSDK.bleManager = [[WTBLECentralManager alloc] init];
        _bleSDK.dataManager = [[WTDataCenterManager alloc] init];
        _bleSDK.bleCallback = _bleSDK.bleManager.callback;
        _bleSDK.dataCallback = _bleSDK.dataManager.callback;
        _bleSDK.param = [[WTBLEParam alloc] init];
        [_bleSDK updateBLECentralCallback];
    });
    return _bleSDK;
}

- (void)updateBLECentralCallback {
    __weak typeof(WTBLESDK) *weakSelf = self;
    self.bleManager.characteristicValueUpdateBlock = ^(CBCharacteristic *characteristic, NSError *error) {
        [weakSelf.dataManager dealWithReadCharacteristicValue:characteristic.value];
    };
}

#pragma mark - WTBLEControlProtocol

- (void)startScan {
    self.bleManager.peripheralFilter = self.param.peripheralFilter;
    [self.bleManager startScan];
}

- (void)cancelScan {
    [self.bleManager cancelScan];
}

- (void)tryConnectPeripheral:(CBPeripheral *)peripheral {
    [self.bleManager tryConnectPeripheral:peripheral];
}

- (void)cancelConnection {
    [self.bleManager cancelConnection];
}

- (void)tryReceiveDataAfterConnected {
    [self.bleManager tryReceiveDataAfterConnected];
}

- (void)writeData:(NSData *)data {
    [self.bleManager writeData:data];
}

#pragma mark - WTBLEApplyProtocol

- (void)sendTempuratureCommand  {
    [self writeData:[self.dataManager assembleTemperatureCommand]];
}

- (void)sendMagneticCommand {
    [self writeData:[self.dataManager assembleMagneticCommand]];
}

- (void)sendBaromemtricPressureCommand {
    [self writeData:[self.dataManager assembleBaromemtricPressureCommand]];
}

- (void)sendPortCommand {
    [self writeData:[self.dataManager assemblePortCommand]];
}

- (void)sendQuaternionCommand {
    [self writeData:[self.dataManager assembleQuaternionCommand]];
}

- (void)accelerometerCali {
    [self writeData:[self.dataManager assembleAccCaliCommand]];
}

- (void)accelerometerCaliL {
    [self writeData:[self.dataManager assembleAccLCaliCommand]];
}

- (void)accelerometerCaliR {
    [self writeData:[self.dataManager assembleAccRCaliCommand]];
}

- (void)magneticCali {
    [self writeData:[self.dataManager assembleMagneticCaliCommand]];
}

- (void)finishMagneticCali {
    [self writeData:[self.dataManager assembleFinishMagneticCaliCommand]];
}

- (void)D0Cali:(WTPortMode)portMode {
    [self writeData:[self.dataManager assembleD0Command:portMode]];
}

- (void)D1Cali:(WTPortMode)portMode {
    [self writeData:[self.dataManager assembleD1Command:portMode]];
}

- (void)D2Cali:(WTPortMode)portMode {
    [self writeData:[self.dataManager assembleD2Command:portMode]];
}

- (void)D3Cali:(WTPortMode)portMode {
    [self writeData:[self.dataManager assembleD3Command:portMode]];
}

- (void)velocityCali:(float)rate {
    [self writeData:[self.dataManager assembleRateCommandWithRate:rate]];
}

- (void)save {
    [self writeData:[self.dataManager assembleSaveCommand:NO]];
}

- (void)resume {
    [self writeData:[self.dataManager assembleSaveCommand:YES]];
}

- (void)changeBLEDeviceName:(NSString *)name {
    NSError *error = [self checkBLENameAvailableWithName:name];
    if (error) {
        if (self.dataCallback.blockOnUpdateBLEName) {
            self.dataCallback.blockOnUpdateBLEName(NO, error);
        }
        return;
    }
    
    dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (int64_t)(0.5 * NSEC_PER_SEC)), dispatch_get_main_queue(), ^{
        if (self.dataCallback.blockOnUpdateBLEName) {
            self.dataCallback.blockOnUpdateBLEName(YES, nil);
        }
    });
    
    [self writeData:[self.dataManager assembleChangeNameCommand:name]];
}

- (NSError *)checkBLENameAvailableWithName:(NSString *)name {
    NSError *error = nil;
    name = [name stringByTrimmingCharactersInSet:[NSCharacterSet whitespaceCharacterSet]];
    if (![name hasPrefix:@"WT"]) {
        NSString *desc = NSLocalizedString(@"prefix_of_ble_device", nil);
        if (desc.length == 0) {
            desc = @"The name should has prefix of 'WT'";
        }
        error = [NSError errorWithDomain:@"com.wit-motion.wtble" code:-1 userInfo:@{NSLocalizedDescriptionKey : desc}];
    } else if ([name lengthOfBytesUsingEncoding:NSUTF8StringEncoding] > 10) {
        NSString *desc = NSLocalizedString(@"length_of_ble_device", nil);
        if (desc.length == 0) {
            desc = @"Length should less than 10 bytes";
        }
        error = [NSError errorWithDomain:@"com.wit-motion.wtble" code:-1 userInfo:@{NSLocalizedDescriptionKey : desc}];
    }
    return error;
}

@end
