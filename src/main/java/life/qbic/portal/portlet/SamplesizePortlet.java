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
@Widgetset("life.qbic.portal.portlet.AppWidgetSet")
public class SamplesizePortlet extends QBiCPortletUI {

  private static final long serialVersionUID = -3245533336166686560L;
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
    }
    OpenBisClient openbis = null;
    try {
      logger.debug("trying to connect to openbis");
      final String baseURL = config.getDataSourceUrl();
      final String apiURL = baseURL + "/openbis/openbis";

      openbis =
          new OpenBisClient(config.getDataSourceUser(), config.getDataSourcePassword(), apiURL);
      openbis.login();
    } catch (Exception e) {
      System.out.println(e.getMessage());
      System.out.println(e.getCause());
      success = false;
      logger.error("User \"" + user
          + "\" could not connect to openBIS and has been informed of this.");
      layout.addComponent(new Label(
          "Data Management System could not be reached. Please try again later or contact us."));
    }

    if (success) {
      new Controller(openbis, layout, config, user);
    }
    layout.setSpacing(true);
    layout.setMargin(true);
    
    return layout;
  }
}
