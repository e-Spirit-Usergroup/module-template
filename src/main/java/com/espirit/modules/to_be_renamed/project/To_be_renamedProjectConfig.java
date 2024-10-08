package com.espirit.modules.to_be_renamed.project;

import com.espirit.ps.psci.genericconfiguration.ExecuteAction;
import com.espirit.ps.psci.genericconfiguration.GenericConfigPanel;
import de.espirit.firstspirit.module.ProjectEnvironment;
import java.util.Locale;
import java.util.ResourceBundle;

public class To_be_renamedProjectConfig extends GenericConfigPanel<ProjectEnvironment> {

    private static final Class<?> LOGGER = To_be_renamedProjectConfig.class;

    private static final String BUNDLE_NAME = "to_be_renamed";
    private static final ResourceBundle resourceBundle = ResourceBundle.getBundle(BUNDLE_NAME, Locale.getDefault());

    public static final String VAR_1 = "var1";
    public static final String VAR_2 = "var2";

    public static final String DOM_COMPONENT = "domComponent";
    public static final String TEXT_COMPONENT = "textComponent";



    @Override
    protected void configure() {

        ExecuteAction ea = () -> {
            try {

                //do something when the configuration is loaded

            }catch (Exception e) {

                //do something, i.e. log error outputs

            }
        };

        //set up a configuration
        //you can use and concatenate several generic fields like text, password, checkbox or hiddenString


        builder().text(resourceBundle.getString("projectConfig.to_be_renamed.var1Label"), VAR_1, "to_be_renamed value", resourceBundle.getString("projectConfig.to_be_renamed.var1Tooltip"))
                .password(resourceBundle.getString("projectConfig.to_be_renamed.var2Label"), VAR_2, "to_be_renamed secret", resourceBundle.getString("projectConfig.to_be_renamed.var2Tooltip"))
                .hiddenString(DOM_COMPONENT, "sc_dom")
                .hiddenString(TEXT_COMPONENT, "sc_text");
    }
}