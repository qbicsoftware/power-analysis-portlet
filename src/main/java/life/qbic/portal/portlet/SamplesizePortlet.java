package life.qbic.portal.portlet;

import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Widgetset;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.Label;
import com.vaadin.ui.Layout;
import com.vaadin.ui.VerticalLayout;

import life.qbic.openbis.openbisclient.OpenBisClient;
import life.qbic.portal.utils.ConfigurationManager;
import life.qbic.portal.utils.ConfigurationManagerFactory;
import life.qbic.portal.utils.LiferayIndependentConfigurationManager;
import life.qbic.portal.utils.PortalUtils;
import life.qbic.samplesize.control.Controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Entry point for portlet samplesize-portlet. This class derives from {@link QBiCPortletUI}, which
 * is found in the {@code portal-utils-lib} library.
 * 
 * @see <a href=https://github.com/qbicsoftware/portal-utils-lib>portal-utils-lib</a>
 */
@Theme("mytheme")
@SuppressWarnings("serial")
@Widgetset("life.qbic.portal.portlet.AppWidgetSet")
public class SamplesizePortlet extends QBiCPortletUI {

  private static final Logger logger = LogManager.getLogger(SamplesizePortlet.class);

  @Override
  protected Layout getPortletContent(final VaadinRequest request) {
    logger.info("Generating content for {}", SamplesizePortlet.class);

    ConfigurationManager config = ConfigurationManagerFactory.getInstance();


    VerticalLayout layout = new VerticalLayout();

    boolean success = true;
    String user = "admin";
    if (PortalUtils.isLiferayPortlet()) {
      user = PortalUtils.getUser().getScreenName();
    } else {
      LiferayIndependentConfigurationManager.Instance.init("local.properties");
      config = LiferayIndependentConfigurationManager.Instance;
    }
    OpenBisClient openbis = null;
    try {
      logger.debug("trying to connect to openbis");
      openbis = new OpenBisClient(config.getDataSourceUser(), config.getDataSourcePassword(),
          config.getDataSourceUrl());
      openbis.login();
    } catch (Exception e) {
      success = false;
      logger.error("User \"" + user
          + "\" could not connect to openBIS and has been informed of this.");
      layout.addComponent(new Label(
          "Data Management System could not be reached. Please try again later or contact us."));
    }
    if (success) {
      Controller contro = new Controller(openbis, layout, config, user);
    }
    layout.setSpacing(true);
    layout.setMargin(true);
    return layout;
  }
}
