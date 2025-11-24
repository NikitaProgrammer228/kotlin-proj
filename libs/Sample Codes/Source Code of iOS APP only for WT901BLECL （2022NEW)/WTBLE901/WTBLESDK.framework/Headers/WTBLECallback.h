//
//  WTBLECallback.h
//  WTBLESDK
//
//  Created by wit-motion on 2019/1/25.
//  Copyright © 2019 wit-motion. All rights reserved.
//

#import <CoreBluetooth/CoreBluetooth.h>

//设备状态改变的委托
typedef void (^WTCentralManagerDidUpdateStateBlock)(CBCentralManager *central);
//找到设备的委托
typedef void (^WTDiscoverPeripheralsBlock)(CBCentralManager *central, CBPeripheral *peripheral, NSDictionary *advertisementData, NSNumber *RSSI);
//连接设备成功的block
typedef void (^WTConnectedPeripheralBlock)(CBCentralManager *central, CBPeripheral *peripheral);
//连接设备失败的block
typedef void (^WTFailToConnectBlock)(CBCentralManager *central, CBPeripheral *peripheral, NSError *error);
//断开设备连接的bock
typedef void (^WTDisconnectBlock)(CBCentralManager *central, CBPeripheral *peripheral, NSError *error);
//找到服务的block
typedef void (^WTDiscoverServicesBlock)(CBPeripheral *peripheral,NSError *error);
//找到Characteristics的block
typedef void (^WTDiscoverCharacteristicsBlock)(CBPeripheral *peripheral,CBService *service,NSError *error);
//更新（获取）Characteristics的value的block
typedef void (^WTReadValueForCharacteristicBlock)(CBPeripheral *peripheral,CBCharacteristic *characteristic,NSError *error);
//获取Characteristics的名称
typedef void (^WTDiscoverDescriptorsForCharacteristicBlock)(CBPeripheral *peripheral,CBCharacteristic *service,NSError *error);
//获取Descriptors的值
typedef void (^WTReadValueForDescriptorsBlock)(CBPeripheral *peripheral,CBDescriptor *descriptor,NSError *error);

typedef void (^WTDidWriteValueForCharacteristicBlock)(CBCharacteristic *characteristic,NSError *error);

typedef void (^WTDidWriteValueForDescriptorBlock)(CBDescriptor *descriptor,NSError *error);

@interface WTBLECallback : NSObject

#pragma mark - callback block
//设备状态改变的委托
@property (nonatomic, copy) WTCentralManagerDidUpdateStateBlock blockOnCentralManagerDidUpdateState;
//发现peripherals
@property (nonatomic, copy) WTDiscoverPeripheralsBlock blockOnDiscoverPeripherals;
//连接callback
@property (nonatomic, copy) WTConnectedPeripheralBlock blockOnConnectedPeripheral;
//连接设备失败的block
@property (nonatomic, copy) WTFailToConnectBlock blockOnFailToConnect;
//断开设备连接的bock
@property (nonatomic, copy) WTDisconnectBlock blockOnDisconnect;
 //发现services
@property (nonatomic, copy) WTDiscoverServicesBlock blockOnDiscoverServices;
//发现Characteristics
@property (nonatomic, copy) WTDiscoverCharacteristicsBlock blockOnDiscoverCharacteristics;
//发现更新Characteristics的
@property (nonatomic, copy) WTReadValueForCharacteristicBlock blockOnReadValueForCharacteristic;
//获取Characteristics的名称
@property (nonatomic, copy) WTDiscoverDescriptorsForCharacteristicBlock blockOnDiscoverDescriptorsForCharacteristic;
//获取Descriptors的值
@property (nonatomic, copy) WTReadValueForDescriptorsBlock blockOnReadValueForDescriptors;

@property (nonatomic, copy) WTDidWriteValueForCharacteristicBlock blockOnDidWriteValueForCharacteristic;

@property (nonatomic, copy) WTDidWriteValueForDescriptorBlock blockOnDidWriteValueForDescriptor;

@end
