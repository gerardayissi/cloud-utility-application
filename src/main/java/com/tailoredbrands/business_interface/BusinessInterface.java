package com.tailoredbrands.business_interface;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BusinessInterface {

    FACILITY("facility"),
    ITEM_DELTA_FEED("item_delta_feed"),
    ITEM_FULL_FEED("item_full_feed");

    private final String name;
}
