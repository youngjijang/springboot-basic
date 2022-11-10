package com.program.voucher.model;

import java.util.UUID;

public interface Voucher {
    UUID getVoucherId();
    String getVoucherType();
    int getVoucherDiscount();
    int discountPrice(int beforeDiscount);

}
