//
//  WTBLECentralManager.m
//  WTBLESDK
//
//  Created by wit-motion on 2019/1/26.
//  Copyright © 2019 wit-motion. All rights reserved.
//

#import "WTBLECentralManager.h"

@interface WTBLECentralManager()<CBCentralManagerDelegate, CBPeripheralDelegate>

@property (nonatomic, strong) CBCentralManager *manager;
@property (nonatomic, strong) CBPeripheral *peripheral;
@property (nonatomic, strong) CBCharacteristic *writeCharacteristic;
@property (nonatomic, strong) CBCharacteristic *readCharacteristic;

@end

@implementation WTBLECentralManager

- (instancetype)init {
    if (self = [super init]) {
        self.manager = [[CBCentralManager alloc] initWithDelegate:self queue:dispatch_get_main_queue()];
        self.callback = [[WTBLECallback alloc] init];
    }
    return self;
}

- (void)startScan {
    NSDictionary *option = @{CBCentralManagerScanOptionAllowDuplicatesKey : [NSNumber numberWithBool:NO],
                             CBCentralManagerOptionShowPowerAlertKey : [NSNumber numberWithBool:YES]};
    [self.manager scanForPeripheralsWithServices:nil options:option];
}

- (void)cancelScan {
    [self.manager stopScan];
}

- (void)tryConnectPeripheral:(CBPeripheral *)peripheral {
    [self cancelConnection];
    self.peripheral = peripheral;
    self.peripheral.delegate = self;
    [self.manager connectPeripheral:peripheral options:nil];
}

- (void)cancelConnection {
    if (self.peripheral) {
        [self.manager cancelPeripheralConnection:self.peripheral];
    }
}

- (void)tryReceiveDataAfterConnected {
    [self.peripheral discoverServices:nil];
}

- (void)writeData:(NSData *)data {
    if (!data) {
        return;
    }
    [self.peripheral writeValue:data
              forCharacteristic:self.writeCharacteristic
                           type:CBCharacteristicWriteWithResponse];
}

#pragma mark - CBCentralManagerDelegate

- (void)centralManagerDidUpdateState:(CBCentralManager *)central {
    switch (central.state) {
        case CBCentralManagerStateUnknown:
            NSLog(@">>>CBCentralManagerStateUnknown");
            break;
        case CBCentralManagerStateResetting:
            NSLog(@">>>CBCentralManagerStateResetting");
            break;
        case CBCentralManagerStateUnsupported:
            NSLog(@">>>CBCentralManagerStateUnsupported");
            break;
        case CBCentralManagerStateUnauthorized:
            NSLog(@">>>CBCentralManagerStateUnauthorized");
            break;
        case CBCentralManagerStatePoweredOff:
            NSLog(@">>>CBCentralManagerStatePoweredOff");
            break;
        case CBCentralManagerStatePoweredOn:
            NSLog(@">>>CBCentralManagerStatePoweredOn");
            break;
        default:
            break;
    }
}

- (void)centralManager:(CBCentralManager *)central didDiscoverPeripheral:(CBPeripheral *)peripheral advertisementData:(NSDictionary<NSString *,id> *)advertisementData RSSI:(NSNumber *)RSSI {
    
    if (self.peripheralFilter.length > 0) {
        if (![peripheral.name isKindOfClass:[NSString class]]
            || peripheral.name.length == 0
            || ![peripheral.name containsString:self.peripheralFilter]) {
            return;
        }
    }
    if (self.callback.blockOnDiscoverPeripherals) {
        self.callback.blockOnDiscoverPeripherals(self.manager, peripheral, advertisementData, RSSI);
    }
}

- (void)centralManager:(CBCentralManager *)central didConnectPeripheral:(CBPeripheral *)peripheral {
    if (self.callback.blockOnConnectedPeripheral) {
        self.callback.blockOnConnectedPeripheral(self.manager, peripheral);
    }
}

- (void)centralManager:(CBCentralManager *)central didFailToConnectPeripheral:(CBPeripheral *)peripheral error:(NSError *)error {
    if (self.callback.blockOnFailToConnect) {
        self.callback.blockOnFailToConnect(self.manager, peripheral, error);
    }
}

- (void)centralManager:(CBCentralManager *)central didDisconnectPeripheral:(CBPeripheral *)peripheral error:(NSError *)error {
    if (self.callback.blockOnDisconnect) {
        self.callback.blockOnDisconnect(self.manager, peripheral, error);
    }
}

- (void)setReadCharacteristic:(CBCharacteristic *)readCharacteristic {
    _readCharacteristic = readCharacteristic;
    [self.peripheral setNotifyValue:YES forCharacteristic:readCharacteristic];
}

- (void)setWriteCharacteristic:(CBCharacteristic *)writeCharacteristic {
    _writeCharacteristic = writeCharacteristic;
}

#pragma mark - CBPeripheralDelegate

// 该方法为 discoverServices:回调
- (void)peripheral:(CBPeripheral *)peripheral didDiscoverServices:(NSError *)error {
    if (error) {
        NSLog(@"discover service error, error is %@", error);
        return;
    }
    
    for (CBService *service in peripheral.services) {
        [peripheral discoverCharacteristics:nil forService:service];
    }
}

// discoverCharacteristics:forService:
- (void)peripheral:(CBPeripheral *)peripheral didDiscoverCharacteristicsForService:(CBService *)service error:(NSError *)error {
    for (CBCharacteristic *characteristic in service.characteristics) {
        NSString *uuidString = characteristic.UUID.UUIDString;
        if ([uuidString.lowercaseString containsString:@"ffe9"]) {
            self.writeCharacteristic = characteristic;
        } else if ([uuidString.lowercaseString containsString:@"ffe4"]) {
            self.readCharacteristic = characteristic;
        }
    }
}

// readValueForCharacteristic:
- (void)peripheral:(CBPeripheral *)peripheral didUpdateValueForCharacteristic:(CBCharacteristic *)characteristic error:(NSError *)error {
    if (self.callback.blockOnReadValueForCharacteristic) {
        self.callback.blockOnReadValueForCharacteristic(peripheral, characteristic, error);
    }
    if (self.characteristicValueUpdateBlock) {
        self.characteristicValueUpdateBlock(characteristic, error);
    }
}

// discoverDescriptorsForCharacteristic:
- (void)peripheral:(CBPeripheral *)peripheral didDiscoverDescriptorsForCharacteristic:(CBCharacteristic *)characteristic error:(NSError *)error {
    if (self.callback.blockOnDiscoverDescriptorsForCharacteristic) {
        self.callback.blockOnDiscoverDescriptorsForCharacteristic(peripheral, characteristic, error);
    }
}

// writeValue:forCharacteristic:type:
- (void)peripheral:(CBPeripheral *)peripheral didWriteValueForCharacteristic:(CBCharacteristic *)characteristic error:(NSError *)error {
    if (self.callback.blockOnDidWriteValueForCharacteristic) {
        self.callback.blockOnDidWriteValueForCharacteristic(characteristic, error);
    }
}

// writeValue:forDescriptor:
- (void)peripheral:(CBPeripheral *)peripheral didWriteValueForDescriptor:(CBDescriptor *)descriptor error:(NSError *)error {
    if (self.callback.blockOnDidWriteValueForDescriptor) {
        self.callback.blockOnDidWriteValueForDescriptor(descriptor, error);
    }
}

@end
