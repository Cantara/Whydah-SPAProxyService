package net.whydah.service.inn.proxy.commands;

import net.whydah.sso.commands.baseclasses.BaseHttpPostHystrixCommand;

import java.net.URI;

public class CommandInnAPIDeleteDeliveryAddress extends BaseHttpPostHystrixCommand<String> {
    int retryCnt = 0;
    private String myApplicationTokenId;
    private String nyUserTokenId;
    private String deliveryAddressLabel;

    public CommandInnAPIDeleteDeliveryAddress(URI serviceUri, String myAppTokenId, String nyUserTokenId, String deliveryAddressLabel) {
        super(serviceUri, null, myAppTokenId, "InnGetaAPI", 6000);
        this.myApplicationTokenId = myAppTokenId;
        this.nyUserTokenId = nyUserTokenId;
        this.deliveryAddressLabel = deliveryAddressLabel;
    }

    @Override
    protected String getTargetPath() {
        return this.myApplicationTokenId + "/api/" + this.nyUserTokenId + "/deliveryaddress/delete/" + deliveryAddressLabel;
    }
}
