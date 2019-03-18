({
    /**
     * Validate action
     **/
    validateAction: function(component) {
        return new Promise(function(resolve, reject) {
        	let params = component.get("v.params");
            if(!params.action || (params.action != 'create_lead' && params.action != 'create_opportunity')){
                reject({
                    title: 'Data Action not supported', 
                    message: 'Only Create Lead and Create Opportunity action are supported.'
                });
            }
            resolve(true);
        });
    },
    /**
     * Get record type Info based on the project data action
     * 
     * @return - recordType info
     **/
    getRecordTypeInfo: function(component, helper) {
        return new Promise(function(resolve, reject) {
            let params = component.get("v.params");
            let objectName;
            //Make server call to get record type info 
                let action = component.get('c.fetchRecordTypeValues');
                action.setParams({
                    objectName: ((params.action == 'create_lead')?'Lead':'Opportunity')
                });
            
                action.setCallback(this, function(response){
                    let state = response.getState();
                    if(state === "SUCCESS") {
                        resolve(response.getReturnValue());
                    }
                    else if(state === "INCOMPLETE") {
                        reject({
                            title: 'Network Error', 
                            message: 'Please try again. If the error persist, please contact system administrator.'
                        });
                    } 
                    else if(state === "ERROR") {
                        let errors = response.getError();
                        if (errors && errors[0] && errors[0].message) {
                            reject({
                                title: 'Error', 
                                message: errors[0].message
                            });
                        }
                        else {
                            reject({
                                title: 'Connection Error', 
                                message: 'Please try again. If the error persist, please contact system administrator.'
                            });
                        }
                    }
                });
                $A.enqueueAction(action);
            
        });  
    },
    
    /**
     * open record type selection from user
     * Allow user to select record type
     * 
     * @param - recordTypeInfo
     **/
    openRecordTypeSelection: function(component, helper, recordTypeInfo) {
        return new Promise(function(resolve) {
            component.set('v.lstOfRecordType', recordTypeInfo);
            helper.toggleRecordTypeSelection(component, true)
            .then(
                $A.getCallback(function(result){ 
                    resolve(true);
                })
            );
        }); 
    },
    
    /**
     * Toggle create sales modal
     * 
     * @param - openModal - set to true to open create sale modal
     **/
    toggleRecordTypeSelection: function(component, openModal) {
		return new Promise($A.getCallback(function(resolve) {
            component.set('v.openRecordTypeSelection', openModal);
            resolve(true);
        }));
    },    
    
    /**
     * Create new record
     * If record type is provide in previous step use it
     * else default recordtype
     * 
     * @param - recordTypeId - record type id
     **/
    createNewRecord: function(component, helper, recordTypeId) {
        return new Promise($A.getCallback(function(resolve, reject){
            let params = component.get("v.params");
            if(params.action == 'create_lead') {
            	
                helper.createLeadRecord(component, helper, recordTypeId)
                .then(
                    $A.getCallback(function(result){
                        resolve(true);
                    })
                )
                .catch(
                    $A.getCallback(function(error){
                        //TODO error handling
                        console.log(error);
                        reject({
                            title: error.title, 
                            message: error.message
                        });
                    })
                );
                return;
            } else if(params.action == 'create_opportunity') {
                /**
                 * For type 'project' can show create opportunity without Account field 
                 * prepopulate
                 * 
                 * For type 'role' and 'tenderer' show account confimation screen
                 * as both has company name field in cordell data
                 * 
                 **/
                if(params.type == 'project') {
                    helper.createOpportunityRecord(component, helper)
                    .catch(
                        $A.getCallback(function(error) {
                            reject({
                                title: error.title, 
                                message: error.message
                            });
                        })
                    );
                }
                else {
                    let accName = '';
                    if(params.type == 'role' 
                       	&& params.data.role.ROLE_COMPANY_NAME__c
                      	&& params.data.role.ROLE_COMPANY_NAME__c != ''
                      ) {
                        accName = params.data.role.ROLE_COMPANY_NAME__c;
                        component.set('v.accName', accName)
                    }
                    else if(params.type == 'tenderer' 
                            && params.data.tenderer.TENDERER_COMPANY_NAME__c
                           	&& params.data.tenderer.TENDERER_COMPANY_NAME__c != ''
                           ) {
                        accName = params.data.tenderer.TENDERER_COMPANY_NAME__c;
                        component.set('v.accName', accName)
                    }
                    helper.getMatchingAccount(component, accName)
                    .then(
                        $A.getCallback(function(result){
                            let showOppButton = false;
                            if(result && result.Id && result.Id !='') {
                                let oppObj = {};
                                oppObj.AccountId = result.Id;
                                component.set('v.oppObj', oppObj);
                                showOppButton = true;
                            }
                            return Promise.all([
                                helper.toggleAccountConfirmation(component, true),
                                showOppButton
                            ]);
                        })
                    )
                    .then(
                        $A.getCallback(function(result) {
                            //toggleOpportunityButton should be executed only after
                            //toggleAccountConfirmation
                            return helper.toggleOpportunityButton(component, result[1]);
                        })
                    )
                    .then(
                        $A.getCallback(function(result){
                            resolve(true);
                        })
                    )
                    .catch(
                        $A.getCallback(function(error){
                            
                            reject({
                                title: error.title, 
                                message: error.message
                            });
                        })
                    );
                    return;
                }
            }
        }));
    },
    
    /**
     * Create Lead method to show create lead modal
     * with the default params and user selected recordtype (if any)
     * 
     * @param - recordTypeId - User provided record id from UI
     * 			else it will be blank
     **/
    createLeadRecord: function(component, helper, recordTypeId) {
        return new Promise($A.getCallback(function(resolve){
            let params = component.get("v.params");
            let crEventParams = {};
            crEventParams.entityApiName = 'Lead';
            
            //Record type event param need to set only if user
            //selected the record type in the UI
            if(recordTypeId && recordTypeId != '') {
                crEventParams.recordTypeId = recordTypeId; 
            }
            
            //Set all defaulFieldValues as param
            //before showing the create record page
            if(params.type == 'project') {
                crEventParams.defaultFieldValues = {
                    Project__c: params.data.project.Id,
                    Title : params.data.project.PROJ_TITLE__c,
                };
            } else if(params.type == 'role') {
                crEventParams.defaultFieldValues = {
                    Project__c: params.data.project.Id,
                    Title: params.data.project.PROJ_TITLE__c,
                    FirstName: params.data.role.ROLE_CONTACT_FIRST_NAME__c,
                    LastName: params.data.role.ROLE_CONTACT_LAST_NAME__c,
                    Company: params.data.role.ROLE_COMPANY_NAME__c,
                    Phone: params.data.role.ROLE_CONTACT_PHONE_NO__c,
                    Fax: params.data.role.ROLE_FAX__c,
                    Email: params.data.role.Role_email_address__c,
                    
                };
            } else if(params.type == 'tenderer') {
                crEventParams.defaultFieldValues = {
                    Project__c: params.data.project.Id,
                    Title: params.data.project.PROJ_TITLE__c,
                    FirstName: params.data.tenderer.Tenderer_contact_first_name__c,
                    LastName: params.data.tenderer.Tenderer_contact_last_name__c,
                    Company: params.data.tenderer.TENDERER_COMPANY_NAME__c,
                    Phone: params.data.tenderer.Tenderer_contact_phone_no__c,
                    Fax: params.data.tenderer.TENDERER_FAX__c,
                    Email: params.data.tenderer.Tenderer_email_address__c,
                };    
            }
            
            //Create record standard event
            Promise.all([
                helper.createRecordEvent(crEventParams),
                helper.closeCreateSalesCmp(component) //Close create sales component
            ])
            .then(
                $A.getCallback(function(result){
                    resolve(true);
                })
            );
        }));
    },
    
    /**
     * Create Opportunity method to show create opportunity modal
     * with the default params and user selected recordtype (if any)
     * 
     * @param - recordTypeId - User provided record id from UI
     * 			else it will be blank
     **/
    createOpportunityRecord: function(component, helper) {
        return new Promise($A.getCallback(function(resolve){
            let params = component.get("v.params");
            
            let oppObj = component.get("v.oppObj");
            
            let crEventParams = {};
            crEventParams.entityApiName = 'Opportunity';
            
            //TODO Based on type change the default values
            
            if(params.type == 'project') {
                //helper.createAccount(component, helper)
                crEventParams.defaultFieldValues = {
                    Project__c: params.data.project.Id,
                    Name : params.data.project.PROJ_TITLE__c,
                };
            } else if(params.type == 'role') {
                
                crEventParams.defaultFieldValues = {
                    
                    Project__c: params.data.project.Id,
                    AccountId: oppObj.AccountId,
                    Name: params.data.project.PROJ_TITLE__c,
                    Amount: params.data.project.PROJ_VALUE__c,
                    
                }; 
            } else if(params.type == 'tenderer') {
                crEventParams.defaultFieldValues = {
                    Project__c: params.data.project.Id,
                    AccountId: oppObj.AccountId,
                    Name : params.data.project.PROJ_TITLE__c,
                    Amount: params.data.project.PROJ_VALUE__c,
                };
            }
            	
            //Create record standard event
            Promise.all([
                helper.createRecordEvent(crEventParams),
                helper.closeCreateSalesCmp(component) //Close create sales component
            ])
            .then(
                $A.getCallback(function(result){
                    resolve(true);
                })
            );
        }));
    },
    
    /**
     * Toggle account confirmation modal
     * 
     * @param - openModal - set to true to open create sale modal
     **/
    toggleAccountConfirmation: function(component, openModal) {
		return new Promise($A.getCallback(function(resolve) {
            component.set('v.openAccountConfirmation', openModal);
            resolve(true);
        }));
    },   
    
    /**
     * Toggle Account and Opportunity button
     * @param - showOppButton 
     * 			- if set to true show opp button and hide account button
     * 			- else show account button and hide opp button
     **/
    toggleOpportunityButton: function(component, showOppButton) {
        return new Promise($A.getCallback(function(resolve){
        	component.find('createCompany').set('v.disabled', (showOppButton));
            component.find('createOpportunity').set('v.disabled', (!showOppButton));
        }));
    },
    
    /**
     * Get Matching account in SF
     **/
    getMatchingAccount: function(component, accName) {
        return new Promise($A.getCallback(function(resolve, reject) {
            if(accName && accName == '') {
                resolve(false);
                return;
            }
            //TODO implement service component for server all
            let action = component.get('c.getMatchingAccount');
            action.setParams({
                accountName: accName
            });
            action.setCallback(this, function(response){
                let state = response.getState();
                if(state === "SUCCESS") {
                    resolve(response.getReturnValue());
                } else if(state === "INCOMPLETE") {
                    reject({
                        title: 'Network Error', 
                        message: 'Please try again. If the error persist, please contact system administrator.'
                    });
                } else if(state === "ERROR") {
                    let errors = response.getError();
                    if (errors && errors[0] && errors[0].message) {
                        reject({
                            title: 'Error', 
                            message: errors[0].message
                        });
                    }
                    else {
                        reject({
                            title: 'Connection Error', 
                            message: 'Please try again. If the error persist, please contact system administrator.'
                        });
                    }
                }
            });
            $A.enqueueAction(action);
        }));
    },
    
    /**
     * Standard Record create event
     * 
     * @param - eventParams - std record create event params
     **/
    createRecordEvent: function(eventParams) {
        return new Promise($A.getCallback(function(resolve){
            //Standard Create Record Event
            let createRecordEvent = $A.get("e.force:createRecord"); 
            
            createRecordEvent.setParams(eventParams); 
            createRecordEvent.fire();
            
            resolve(true);
        }));
    },
    
    /**
     * close create sale component 
     * from parent component
     * 
     **/
    closeCreateSalesCmp: function(component) {
        return new Promise($A.getCallback(function(resolve){
            let pCmp = component.get('v.parentComponent');
            pCmp.closeCreateSalesCmp();
            resolve(true);
        }));
    },
    
    /**
     * Create Account if when create opportunity account is empty
     * will be populated 
     * 
     * */
    /** TODO develop this method Account defalut values set **/
    /**createAccount: function(component, helper) {
        return new Promise($A.getCallback(function(resolve){
            let params = component.get("v.params");
            console.log('createAccount');
            let crEventParams = {};
            
            if(params.action == 'create_opportunity') {
            	crEventParams.entityApiName = 'Account';
                //TODO Based on type change the default values
                if(params.type == 'role') {
                    crEventParams.defaultFieldValues = {
                        Name: params.data.ROLE_COMPANY_NAME__c,
                        BillingAddress: params.data.ROLE_ADDRESS1__c,
                        BillingAddress: params.data.Role_address2__c,
                        BillingCity: params.data.ROLE_SUBURB__c,
                        BillingStateCode: params.data.ROLE_STATE_CODE__c,
                        postcode: params.data.ROLE_POSTCODE__c,
                        phone: params.data.ROLE_PHONE_NO__c,
                    };
                } else if(params.type == 'tenderer') {
                    crEventParams.defaultFieldValues = {
                        Name: params.data.TENDERER_COMPANY_NAME__c,
                        BillingAddress: params.data.TENDERER_ADDRESS1__c,
                        BillingAddress: params.data.Tenderer_address2__c,
                        BillingCity: params.data.TENDERER_SUBURB__c,
                        BillingStateCode: params.data.TENDERER_STATE_CODE__c,
                        postcode: params.data.TENDERER_POSTCODE__c,
                        phone: params.data.TENDERER_PHONE_NO__c,
                    };
                }
            }
            
            //Create record standard event
            Promise.all([
                helper.createRecordEvent(crEventParams),
                helper.closeCreateSalesCmp(component) //Close create sales component
            ])
            .then(
                $A.getCallback(function(result){
                    resolve(true);
                })
            );
        }));
    },**/
})