/*
*Create sales lead and opportunity
*
*
* -- @Ragav
*/
({
    /**
     * Initialze the component by fetching recordtypeinfo
     * And set the UI to get the recordtype from user
     * If no record type user should be prompt to create record
     **/
    doInit: function (component, event, helper) {
        //let parentCmp = component.get("v.parentComponent");
        //parentCmp.viewProjectDetail(row.PROJ_PROJECT_ID, isSynced);
       	
        // Create company will be open new tab
        let navService = component.find("navService");
        let pageReference = {
            type: 'standard__objectPage',
            attributes: {
                objectApiName: 'Account',
                actionName: 'home'
            }
        };
        component.set("v.pageReference", pageReference);
        // Set the URL on the link or use the default if there's an error
        let defaultUrl = "#";
        navService.generateUrl(pageReference)
            .then($A.getCallback(function(url) {
                component.set("v.accounturl", url ? url : defaultUrl);
            }), $A.getCallback(function(error) {
                component.set("v.accounturl", defaultUrl);
            }));
        
        helper.validateAction(component)
        .then(
            $A.getCallback(function(result){ 
                return helper.getRecordTypeInfo(component, helper);
        	})
        )
        .then(
        	$A.getCallback(function(result){ 
                // result should have recordTypeInfo
                // if recordTypeInfo is available all user to select recordType
                if(result && result.length > 1) {
                    return helper.openRecordTypeSelection(component, helper, result);
                }
                let recordTypeId = '';
                if(result && result.length == 1) {
                    recordTypeId = result[0].Id;
                }
                return helper.createNewRecord(component, helper, '');
        	})
        )
        .catch(
            $A.getCallback(function(error) {
                //TODO Error handling from parent component
                console.log(error);
            })
        );
    },
    
    // After If lead record type selected
    afterRecordTypeSelection: function(component, event, helper) {
      	Promise.all([
            helper.toggleRecordTypeSelection(component, false),
            helper.createNewRecord(component, helper, component.get("v.radioValue"))
        ])
        .then(
            $A.getCallback(function(result) {
                helper.closeCreateSalesCmp(components);
            })
        )
        .catch(
            $A.getCallback(function(error) {
                //TODO Error handling from parent component
            })
        );
    },
    
    // Create Account
    createAccount: function(component, event, helper) {
    	helper.createAccount(component, helper)  
        .catch(
            $A.getCallback(function(error) {
                //TODO Error handling from parent component
                console.log(error);
            })
        );
    },
    
    /**
     * Create Opportunity with confirmed account
     **/
    //TODO validation for Account
    createOpportunity: function(component, event, helper) {
        
        helper.createOpportunityRecord(component, helper)
                
        .catch(
            $A.getCallback(function(error) {
                //TODO Error handling from parent component
                console.log(error);
            })
        );
    },
    
  	// Onchange get recordtype 
  	getRecordType : function(component, event, helper){
        let recordTypeId = event.getSource().get("v.text");
        component.set("v.radioValue", recordTypeId );
    },
    
    // Create new account and redirect the account creation new tab
    /*createAccountSelection: function(component, event, helper){
        let navService = component.find("navService");
        let pageReference = {
            type: 'standard__objectPage',
            attributes: {
                objectApiName: 'Account',
                actionName: 'home',
                target: '_blank'
            }
        };
        console.log(component.get("v.accounturl"));
        navService.navigate(pageReference, true); 
        //let currentLocation = location.href;
        //console.log(currentLocation);
        //console.log('currentLocation');
        // window.open("https://ccv2-dev-ed.lightning.force.com/lightning/o/Account/list?filterName=Recent", '_blank');
        //helper.createAccount(component, event, helper);
    },*/
    
    /**
     * On account change we need to toggle opportunity button
     * if value is null or empty hide Create Opportunity Button
     **/
    onAccountChange: function(component, event, helper) {
        //let value = event.getSource().get('v.value');
        let oppObj = component.get('v.oppObj');
        //component.set("v.oppObje", oppObj.AccountId[0]);
        let hasValue = false;
        if(Object.prototype.toString.call(oppObj.AccountId) === '[object Array]') {
            oppObj.AccountId = oppObj.AccountId[0];
        }
        if(oppObj && oppObj.AccountId) {
            hasValue = true;
        }
        
        helper.toggleOpportunityButton(
            component
            , hasValue
        );
    },
    
    // Modal popup close
    closeRecordTypesSelction: function(component, event, helper) {
        helper.closeCreateSalesCmp(component);
    },
    
 })