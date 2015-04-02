package com.smartbear.ready.plugin.jira.actions;

import com.eviware.soapui.model.ModelItem;
import com.eviware.soapui.plugins.ActionConfiguration;
import com.smartbear.ready.functional.actions.FunctionalActionGroups;
import com.smartbear.ready.license.annotation.ClassRequiresLicense;
import com.smartbear.ready.license.protection.LicensedModule;

/**
 * Created by avdeev on 30.03.2015.
 */
@ActionConfiguration(actionGroup = FunctionalActionGroups.FUNCTIONAL_MODULE_TOOLBAR_ACTIONS, targetType = ModelItem.class, isToolbarAction = true,
        iconPath = "com/smartbear/ready/plugin/jira/icons/Create-new-bug-tracker-issue-icon_20-20-px.png")
@ClassRequiresLicense(validModules = LicensedModule.SoapUIPro)
public class SoapUINGAction extends CreateNewBugAction {
}