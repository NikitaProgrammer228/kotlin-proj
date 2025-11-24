//
//  WTDataCenterManager.h
//  WTBLESDK
//
//  Created by wit-motion on 2019/1/26.
//  Copyright © 2019 wit-motion. All rights reserved.
//

#import <Foundation/Foundation.h>
#import "WTDataCenterCallback.h"

@class WTDataCenterManager;

@interface WTDataCenterManager : NSObject

@property (nonatomic, strong) WTDataCenterCallback *callback;

- (void)dealWithReadCharacteristicValue:(NSData *)data;
- (NSData *)assembleMagneticCommand;
- (NSData *)assembleBaromemtricPressureCommand;
- (NSData *)assemblePortCommand;
- (NSData *)assembleQuaternionCommand;
- (NSData *)assembleTemperatureCommand;

- (NSData *)assembleAccCaliCommand;
- (NSData *)assembleAccLCaliCommand;
- (NSData *)assembleAccRCaliCommand;
- (NSData *)assembleMagneticCaliCommand;
- (NSData *)assembleFinishMagneticCaliCommand;

- (NSData *)assembleSaveCommand:(BOOL)isDefault;

// 0x01:0.1Hz 0x02:0.5Hz 0x03:1Hz 0x04:2Hz 0x05:5Hz 0x06:10Hz(默认) 0x07:20Hz 0x08:50Hz 0x09:100Hz
- (NSData *)assembleRateCommandWithRate:(float)rate;

- (NSData *)assembleD0Command:(WTPortMode)portMode;
- (NSData *)assembleD1Command:(WTPortMode)portMode;
- (NSData *)assembleD2Command:(WTPortMode)portMode;
- (NSData *)assembleD3Command:(WTPortMode)portMode;

- (NSData *)assembleChangeNameCommand:(NSString *)name;

@end
