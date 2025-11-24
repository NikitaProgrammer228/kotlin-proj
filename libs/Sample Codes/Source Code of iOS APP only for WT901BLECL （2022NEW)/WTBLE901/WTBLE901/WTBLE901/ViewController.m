//
//  ViewController.m
//  WTBLE901
//
//  Created by wit-motion on 2019/1/29.
//  Copyright © 2019 wit-motion. All rights reserved.
//

#import "ViewController.h"
#import "DetailViewController.h"
#import "WTBLEPeripheral.h"

@interface ViewController ()<UITableViewDelegate, UITableViewDataSource> {
    NSInteger _selectIndex;
}

@property (weak, nonatomic) IBOutlet UITableView *tableView;
@property (nonatomic, strong) NSMutableArray<WTBLEPeripheral *> *peripheralList;
@property (nonatomic, strong) UIActivityIndicatorView *indicatorView;
@property (nonatomic, strong) UIButton *scanButton;

@end

@implementation ViewController

- (NSMutableArray *)peripheralList {
    if (!_peripheralList) {
        _peripheralList = [[NSMutableArray alloc] init];
    }
    return _peripheralList;
}

- (UIActivityIndicatorView *)indicatorView {
    if (!_indicatorView) {
        _indicatorView = [[UIActivityIndicatorView alloc] initWithActivityIndicatorStyle:UIActivityIndicatorViewStyleGray];
        _indicatorView.hidesWhenStopped = YES;
    }
    return _indicatorView;
}

- (UIButton *)scanButton {
    if (!_scanButton) {
        _scanButton = [UIButton buttonWithType:UIButtonTypeCustom];
        [_scanButton addTarget:self action:@selector(onScanButtonAction:) forControlEvents:UIControlEventTouchUpInside];
        [_scanButton setTitleColor:[UIColor blackColor] forState:UIControlStateNormal];
        [_scanButton setTitleColor:[UIColor blackColor] forState:UIControlStateSelected];
        [_scanButton setTitle:NSLocalizedString(@"stop_scan", nil) forState:UIControlStateNormal];
        [_scanButton setTitle:NSLocalizedString(@"scan", nil) forState:UIControlStateSelected];
    }
    return _scanButton;
}

- (void)viewDidLoad {
    [super viewDidLoad];
    // Do any additional setup after loading the view, typically from a nib.
    
    [self customRightButtonItems];
    [WTBLESDK sharedInstance].param.peripheralFilter = @"WT";
    
    __weak typeof(ViewController) *weakSelf = self;
    [WTBLESDK sharedInstance].bleCallback.blockOnDiscoverPeripherals = ^(CBCentralManager *central, CBPeripheral *peripheral, NSDictionary *advertisementData, NSNumber *RSSI) {
        [weakSelf addPeripheralDeviceWith:peripheral advertisementData:advertisementData RSSI:RSSI];
    };
}

- (void)viewWillAppear:(BOOL)animated {
    [super viewWillAppear:animated];
    self.title = @"BLE Device Scan";
    
    dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (int64_t)(1 * NSEC_PER_SEC)), dispatch_get_main_queue(), ^{
        self.scanButton.selected = YES;
        [self onScanButtonAction:self.scanButton];
    });
}

- (void)viewWillDisappear:(BOOL)animated {
    [super viewWillDisappear:animated];
    self.title = @"";
}

- (void)customRightButtonItems {
    UIBarButtonItem *item0 = [[UIBarButtonItem alloc] initWithCustomView:self.indicatorView];
    UIBarButtonItem *item1 = [[UIBarButtonItem alloc] initWithCustomView:self.scanButton];
    self.navigationItem.rightBarButtonItems = @[item1, item0];
    [self.indicatorView startAnimating];
}

- (void)onScanButtonAction:(UIButton *)sender {
    sender.selected = !sender.isSelected;
    
    if (sender.isSelected) {
        [[WTBLESDK sharedInstance] cancelScan];
        [self.indicatorView stopAnimating];
    } else {
        [self.peripheralList removeAllObjects];
        [self.tableView reloadData];
        [[WTBLESDK sharedInstance] startScan];
        [self.indicatorView startAnimating];
    }
}

- (void)addPeripheralDeviceWith:(CBPeripheral *)peripheral advertisementData:(NSDictionary *)advertisementData RSSI:(NSNumber *)RSSI {
    
    for (WTBLEPeripheral *currentPeripheral in self.peripheralList) {
        if ([currentPeripheral.peripheral.identifier.UUIDString isEqualToString:peripheral.identifier.UUIDString]) {
            return;
        }
    }
    WTBLEPeripheral *blePeripheral = [WTBLEPeripheral peripheralWithCBPeripheral:peripheral
                                                               advertisementData:advertisementData
                                                                            RSSI:RSSI];
    if (blePeripheral) {
        [self.peripheralList addObject:blePeripheral];
        [self.tableView reloadData];
    }
}

- (void)beginConnectPeripheralWith:(WTBLEPeripheral *)peripheral {
    if (!self.scanButton.isSelected) {
        [self onScanButtonAction:self.scanButton];
    }
    [self performSegueWithIdentifier:@"DetailSegue" sender:self];
}

- (void)prepareForSegue:(UIStoryboardSegue *)segue sender:(id)sender {
    if ([segue.identifier isEqualToString:@"DetailSegue"]) {
        if (self.peripheralList.count > _selectIndex) {
            WTBLEPeripheral *blePeripheral = [self.peripheralList objectAtIndex:_selectIndex];
            DetailViewController *controller = segue.destinationViewController;
            controller.peripheral = blePeripheral;
        }
    }
}

#pragma mark - UITableViewDelegate & UITableViewDataSource
- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section {
    return self.peripheralList.count;
}

- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath {
    UITableViewCell *cell = [tableView dequeueReusableCellWithIdentifier:@"BLEDeviceListCell"];
    if (!cell) {
        cell = [[UITableViewCell alloc] initWithStyle:UITableViewCellStyleSubtitle reuseIdentifier:@"BLEDeviceListCell"];
        cell.selectionStyle = UITableViewCellSelectionStyleNone;
    }
    
    WTBLEPeripheral *blePeripheral = [self.peripheralList objectAtIndex:indexPath.row];
    NSString *deviceName = [blePeripheral.advertisementData objectForKey:@"kCBAdvDataLocalName"] ?: (blePeripheral.peripheral.name ?: @"Unknown device");
    NSString *title = deviceName;
    title = [NSString stringWithFormat:@"%@ RSSI:%@", title, blePeripheral.RSSI];
    cell.textLabel.text = title;
    cell.detailTextLabel.text = blePeripheral.peripheral.identifier.UUIDString;
    
    return cell;
}

- (void)tableView:(UITableView *)tableView didSelectRowAtIndexPath:(NSIndexPath *)indexPath {
    WTBLEPeripheral *blePeripheral = [self.peripheralList objectAtIndex:indexPath.row];
    [self beginConnectPeripheralWith:blePeripheral];
}

@end
