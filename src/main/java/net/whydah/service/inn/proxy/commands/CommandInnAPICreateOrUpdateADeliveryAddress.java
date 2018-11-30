package net.whydah.service.inn.proxy.commands;

import net.whydah.sso.commands.baseclasses.BaseHttpPostHystrixCommand;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class CommandInnAPICreateOrUpdateADeliveryAddress extends BaseHttpPostHystrixCommand<String> {
    int retryCnt = 0;
    private String myApplicationTokenId;
    private String nyUserTokenId;

    String tag;
    String companyName;
    String emailAddress;
    String contactName;
    String phoneNumber;
    String countryCode;
    String postalCode;
    String postalCity;
    String mainAddressLine;
    String addressLine1;
    String addressLine2;
    String comment;
    boolean useAsMainAddress;
    boolean select;

    public CommandInnAPICreateOrUpdateADeliveryAddress(URI serviceUri, String myAppTokenId, String nyUserTokenId,
                                                       String tag, String companyName, String emailAddress, String contactName, String phoneNumber,
                                                       String countryCode, String postalCode, String postalCity, String mainAddressLine,
                                                       String addressLine1, String addressLine2, String comment, boolean useAsMainAddress, boolean select) {
        super(serviceUri, null, myAppTokenId, "InnGetaAPI", 10000);
        this.myApplicationTokenId = myAppTokenId;
        this.nyUserTokenId = nyUserTokenId;
        this.tag = tag;
        this.useAsMainAddress = useAsMainAddress;
        this.companyName = companyName;
        this.emailAddress = emailAddress;
        this.contactName = contactName;
        this.phoneNumber = phoneNumber;
        this.countryCode = countryCode;
        this.postalCode = postalCode;
        this.postalCity = postalCity;
        this.mainAddressLine = mainAddressLine;
        this.addressLine1 = addressLine1;
        this.addressLine2 = addressLine2;
        this.comment = comment;
        this.select = select;
    }

    @Override
    protected String getTargetPath() {
        if (select) {
            return this.myApplicationTokenId + "/api/" + this.nyUserTokenId + "/deliveryaddress/addAndSelect";
        } else {
            return this.myApplicationTokenId + "/api/" + this.nyUserTokenId + "/deliveryaddress/add";
        }
    }

    @Override
    protected Map<String, String> getFormParameters() {
        Map<String, String> data = new HashMap<String, String>();
        data.put("tag", tag);
        data.put("default", String.valueOf(useAsMainAddress));
        data.put("company", companyName);
        data.put("email", emailAddress);
        data.put("name", contactName);
        data.put("cellPhone", phoneNumber);
        data.put("countryCode", countryCode);
        data.put("postalCode", postalCode);
        data.put("postalCity", postalCity);
        data.put("addressLine", mainAddressLine);
        data.put("addressLine1", addressLine1);
        data.put("addressLine2", addressLine2);
        data.put("comment", comment);
        data.put("select", String.valueOf(select));

        return data;
    }
}
