//
//  WTBLEPeripheral.h
//  WTBLE901
//
//  Created by wit-motion on 2019/1/29.
//  Copyright © 2019 wit-motion. All rights reserved.
//

#import <Foundation/Foundation.h>
#import <WTBLESDK/WTBLESDK.h>

@interface WTBLEPeripheral : NSObject

@property (nonatomic, strong) CBPeripheral *peripheral;
@property (nonatomic, strong) NSDictionary *advertisementData;
@property (nonatomic, strong) NSNumber *RSSI;

+ (WTBLEPeripheral *)peripheralWithCBPeripheral:(CBPeripheral *)peripheral
                              advertisementData:(NSDictionary *)advertisementData
                                           RSSI:(NSNumber *)RSSI;
@end
