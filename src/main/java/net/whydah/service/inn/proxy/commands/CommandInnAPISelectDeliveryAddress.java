package net.whydah.service.inn.proxy.commands;

import net.whydah.sso.commands.baseclasses.BaseHttpGetHystrixCommand;

import java.net.URI;

public class CommandInnAPISelectDeliveryAddress extends BaseHttpGetHystrixCommand<String> {
    int retryCnt = 0;
    private String myApplicationTokenId;
    private String nyUserTokenId;
    private String deliveryAddressLabel;

    public CommandInnAPISelectDeliveryAddress(URI serviceUri, String myAppTokenId, String nyUserTokenId, String deliveryAddressLabel) {
        super(serviceUri, null, myAppTokenId, "InnGetaAPI", 6000);
        this.myApplicationTokenId = myAppTokenId;
        this.nyUserTokenId = nyUserTokenId;
        this.deliveryAddressLabel = deliveryAddressLabel;
    }

    @Override
    protected String getTargetPath() {
        if (deliveryAddressLabel != null && deliveryAddressLabel.length() > 0) {
            return this.myApplicationTokenId + "/spasession/" + this.nyUserTokenId + "/select_deliveryaddress/" + deliveryAddressLabel;
        } else {
            return this.myApplicationTokenId + "/spasession/" + this.nyUserTokenId + "/select_default_deliveryaddress";
        }
    }
}
