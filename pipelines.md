# [01 SCA DO](https://wiki.tailoredbrands.com/display/EIS/SCA+DO+To+Fulfillment)
- Details: `DOM - MAO,	Asynchronous, Near Real Time, XML - JSON, ESB - Dataflow - PubSub`
- Status: ready for development 
- [Source file](data/01/SCADO-Source-Sample.xml)
- [Target file](data/01/SCADO-Target-Sample.json)
- [Mapping](data/01/SCA_DO_To_Fulfillment_Mapping_Document.xlsx)
- [Jira](https://jira.tailoredbrands.com/browse/CIS-4)

# [02 DC DO](https://wiki.tailoredbrands.com/pages/viewpage.action?spaceKey=EIS&title=DC+DO+To+Fulfillment)
- Details: `DOM	0- MAO, Asynchronous, Near Real Time, XML - JSON, ESB - Dataflow - PubSub`
- Status: ready for development
- [Source file](data/02/DCDO-Source-Sample.xml)
- [Target file](data/02/DCDO-Target-Sample.json) 
- [Other examples](data/02/dc_do_n_02)
- [Mapping](data/02/DO_To_Fulfillment_Mapping_Document.xlsx)
- [Jira](https://jira.tailoredbrands.com/browse/CIS-5)

# [03 MAO Outbound](https://wiki.tailoredbrands.com/pages/viewpage.action?spaceKey=JBOM&title=Interface+Design+-+2#InterfaceDesign2-4.1.3FULFILLMENTTOASN)
- Details: `MAO	- DOM, Asynchronous, Near Real Time, JSON - XML, PubSub - Dataflow - MQ`
- Status: requirements gathering
- [Source file](data/03/mao_outbound_n_03.zip#mao_outbound_n_03/FulfillmentUpdate_MAO_To_DOM/1/1_1_2000_DOM_DO_Update_RELEASED.xml)
- [Target file](data/03/mao_outbound_n_03.zip#mao_outbound_n_03/FulfillmentUpdate_MAO_To_DOM/1/MAO_JSON_1_1.json)
- [Mapping](data/03/4.1.3.%20Fulfillment%20to%20ASN%20-%20SIF%20to%20SCA%20v1.0.xlsx)
#### **NEEDED** 
- Conditions rules (Target file generates by some rules. It is something about conditions with prefix "1_1")


# [04 Item Image](https://wiki.tailoredbrands.com/display/EIS/Item+Image+Full+Feed)
- Details: `ECOM - MAO, Batch, Nightly, CSV - JSON, File - Dataflow - PubSub, 500K in 10 mins`
- Status: ready for development
- [Source file](data/04/MW_ITEM_IMAGE_1.csv)
- [Target file](data/04/item_image_json.json)
- [Mapping](data/04/Item_Full_Mapping_Document.xlsx)
- [Jira](https://jira.tailoredbrands.com/browse/CIS-6) 
- CSV file with one field. Data of this field is json.
- Output “data” is encoded string “java.util.Base64.getEncoder().encodeToString(messageBody.getBytes())”

# [05 DOM ASN](https://wiki.tailoredbrands.com/display/EIS/DOM+ASN+To+Fulfillment)
- Details: `DOM- MAO, Asynchronous, Near Real Time, XML - JSON, ESB - Dataflow - PubSub`
- Status: ready for development
- [Jira](https://jira.tailoredbrands.com/browse/CIS-10)
- [Mapping](data/05/DOM_ASN_To_Fulfillment_Mapping_Document.xlsx)
- [Source file](data/05/dom_asn_2835400_source.xml)
- [Target file](data/05/dom_asn_2835400_target.json)

# [06 WM ASN](https://wiki.tailoredbrands.com/display/EIS/WMOS+ASN+To+Fulfillment)
- Details: `WM - MAO, Asynchronous, Near Real Time, XML - JSON, ESB - Dataflow - PubSub`
- Status: requirements gathering
- [Jira](https://jira.tailoredbrands.com/browse/CIS-11)
- [Mapping](data/06/WMOS_ASN_To_Fulfillment_Mapping_Document.xlsx)
- [Source file](data/06/wm_asn_J003831665_source.xml)
- [Target file](data/06/wm_asn_J003831665_target.json)

# [07 Item Full Feed](https://wiki.tailoredbrands.com/display/EIS/Item+Full+Feed)
- Details: `Universe - MAO, Batch, On-Demand, CSV - JSON, File - Dataflow - PubSub, 2M in 15 mins`
- Status: in development
- [Source file](data/07/TMW_PRD_ITEM_CODE_3_07212019.csv.zip)
- [Target file](data/07/item_full_feed.json)
- [Mapping](data/07/Item_Full_Mapping_Document.xlsx)
- [Jira](https://jira.tailoredbrands.com/browse/CIS-7)

# [08 DO Partial Short](https://wiki.tailoredbrands.com/display/EIS/DO+Partial+Short)
- Details: `DOM - MAO, Asynchronous, Near Real Time, DB - JSON, DB - PubSub`
- Status: requirements gathering
#### **NEEDED**
 - ~~Source file~~ _is source result query from DB?_
 - ~~Target file~~ _is on the wiki page as an image_
 - ~~Mapping~~ _is on the wiki page as an image_
 - What DB is? Oracle
 
```sql
select o.tc_order_id, 
    oli.TC_ORDER_LINE_ID, 
    O.TC_COMPANY_ID, 
    o.last_updated_dttm
from dom.orders o, 
    dom.order_line_item oli
where o.order_id = oli.order_id
    and o.do_status != 200
    and oli.do_dtl_status = '200'
    and o.created_dttm > TO_DATE('${exchangeProperty.start_date_1}','MMDDYY HH24:MI:SS')
    and (o.last_updated_dttm > sysdate - ${exchangeProperty.time_interval})
    and o.destination_action = '02'
UNION ALL
select o.tc_order_id, 
    oli.TC_ORDER_LINE_ID, 
    O.TC_COMPANY_ID, 
    o.last_updated_dttm
from DOM.ORDERS o, 
    DOM.ORDER_LINE_ITEM oli
where o.order_id = oli.ORDER_ID 
    and o.do_status != 200
    and oli.do_dtl_status = 200
    and (o.created_dttm > '${exchangeProperty.start_date_2}' 
        and (o.last_updated_dttm > sysdate - ${exchangeProperty.time_interval})) 
    and o.destination_action <> '02'
```

# [09 Facility](https://wiki.tailoredbrands.com/display/EIS/Facility)
- Details: `Universe - MAO, Asynchronous, Near Real Time, XML - JSON, ESB - Dataflow - PubSub`
- Status: in development
- [Jira](https://jira.tailoredbrands.com/browse/CIS-8)
- ~~Source file~~
- ~~Target file~~
- ~~Mapping~~
- ~~XSD schema~~

# [10 Item Delta Feed](https://wiki.tailoredbrands.com/display/EIS/Item+Delta+Feed)
- Details: `Universe - MAO, Asynchronous, Near Real Time, XML - JSON, ESB - Dataflow - PubSub`
- Status: ready for development
- [Jira](https://jira.tailoredbrands.com/browse/CIS-9)
- ~~Source file~~
- ~~Target file~~
- ~~XSD schema~~

# [11 User](https://wiki.tailoredbrands.com/pages/viewpage.action?spaceKey=JBOM&title=Interface+Design+-+1#InterfaceDesign1-0.3.1USER,ORGUSERANDUSERROLE)
- Details: `Peoplesoft- MAO, Batch, Nightly, DB - JSON, DB - PubSub, 200K < 15 mins`
- Status: requirements gathering
- [Source file](data/11/TMW_User_Source.xml)
- [Target file](data/11/user_Target.json)
- [Mapping OrgUser](data/11/0.3.1.%20Create%20OrgUser%20-%20PeopleSoft%20to%20MAO%20v1.0.xlsx)
- [Mapping User](data/11/0.3.1.%20Create%20User%20-%20PeopleSoft%20to%20MAO%20v1.0.xlsx)
- [Mapping UserRole](data/11/0.3.1.%20Create%20UserRole%20-%20PeopleSoft%20to%20MAO%20v1.0.xlsx)
#### **NEEDED**
- Gerard mentioned about something 


# [12 Receipts to Close](https://wiki.tailoredbrands.com/display/EIS/Receipts+to+Close) 
- Details: `DOM - MAO, Asynchronous, Near Real Time, XML - JSON, ESB - Dataflow - PubSub`
- Status: Do we need to do a call to DB?
- [Jira](https://jira.tailoredbrands.com/browse/CIS-12)
- [Source file](data/12/Receipts_To_close_Input.xml)
- [Target file](data/12/Receipts_To_close_Output.json)
#### **NEEDED**
 - ~~Mapping~~ is not clear. _is on the wiki page as an image_
 - What DB is? Oracle

* This is the Distribution Order Management (DOM) database. Credentials are as follow:
* Server: dom12tstdb01.tmw.com
* SID: domtest
* Port: 2494
* Username: esbuser
* Password: load4esb
* Schema: dom

```sql
select 
    decode(lpn.tc_company_id, 1, 'TMW', 'JAB') as OrganizationId, 
    lpn.tc_order_id as FulfillmentId, 
    ldet.tc_order_line_id as FulfillmentLineId, 
    ldet.shipped_qty as ShippedQty 
from dom.lpn lpn, dom.lpn_detail ldet 
where lpn.lpn_id = ldet.lpn_id and 
    lpn.tc_lpn_id in ('${exchangeProperty.tc_lpn_id}') 
order by FulfillmentLineId
```

# [13 DO Complete Short](https://wiki.tailoredbrands.com/display/EIS/DO+Complete+Short)
- Details: `DOM - MAO, Asynchronous, Near Real Time, DB - JSON, DB - PubSub`
- Status: requirements gathering
#### **NEEDED**
 - ~~Source file~~ _is source result query from DB?_
 - ~~Target file~~ _is on the wiki page as an image_
 - ~~Mapping~~ _is on the wiki page as an image_
 - What DB is? Oracle

* This is the Distribution Order Management (DOM) database. Credentials are as follow:
* Server: dom12tstdb01.tmw.com
* SID: domtest
* Port: 2494
* Username: esbuser
* Password: load4esb
* Schema: dom


```sql
select o.tc_order_id,
    i.tc_order_line_id,
    o.TC_COMPANY_ID 
from dom.orders o,
    dom.order_line_item i,
    dom.lpn l,
    dom.do_status s1,
    dom.do_status s2 
where o.order_id=i.order_id and 
    o.order_id=l.order_id(+) and 
    o.do_status=s1.order_status and 
    i.do_dtl_status=s2.order_status and 
    o.do_status='200' and 
        (o.created_dttm > '${exchangeProperty.start_date}' and 
        o.last_updated_dttm > sysdate - '${exchangeProperty.time_interval}')
order by o.tc_order_id,i.tc_order_line_id
```

# [14 Selfie Order Status Update](https://wiki.tailoredbrands.com/display/EIS/Selfie+Order+Status+Update)
- Details: `DOM	- MAO, Asynchronous, Near Real Time, DB - JSON, DB - PubSub`
- Status: requirements gathering
#### **NEEDED**
 - ~~Source file~~ _is source result query from DB?_
 - ~~Target file~~ _is on the wiki page as an image_
 - ~~Mapping~~ _is on the wiki page as an image_
 - What DB is? Oracle

This is the Distribution Order Management (DOM) database. Credentials are as follow:
Server: dom12tstdb01.tmw.com
SID: domtest
Port: 2494
Username: esbuser
Password: load4esb
Schema: dom

```sql
select decode(ord.tc_company_id, 1, 'TMW', 'JAB') as OrganizationId, 
    ord.tc_order_id as FulfillmentId, 
    oli.tc_order_line_id as FulfillmentLineId, 
    oli.shipped_qty as ShippedQty
from dom.orders ord, 
    dom.order_line_item oli
where ord.order_id = oli.order_id
    and ord.tc_order_id = '${TC_ORDER_ID}'
    and purchase_order_line_number = '${TC_PO_LINE_ID}'
```


