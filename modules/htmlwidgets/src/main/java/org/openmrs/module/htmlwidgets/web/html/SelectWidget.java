package org.openmrs.module.htmlwidgets.web.html;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.openmrs.module.htmlwidgets.web.WidgetConfig;

/** This represents a select list widget */
public class SelectWidget extends CodedWidget {

  /**
   * @see CodedWidget#render(WidgetConfig)
   */
  @Override
  public void render(WidgetConfig config, Writer w) throws IOException {

    // Open Select Tag
    HtmlUtil.renderOpenTag(w, "select", config.getAttributes());

    Label currentGroup = null;
    boolean inGroup = false;

    for (Option option : getOptions()) {

      // Open New Option Group if Appropriate
      if (option.getGroup() != null) {
        if (!option.getGroup().equals(currentGroup)) {
          currentGroup = option.getGroup();
          if (inGroup) {
            HtmlUtil.renderCloseTag(w, "optgroup");
          }
          List<Attribute> atts = new ArrayList<Attribute>();
          atts.add(new Attribute("label", currentGroup.getLabel(), null, null));
          HtmlUtil.renderOpenTag(w, "optgroup", atts);
        }
        inGroup = true;
      } else {
        if (inGroup) {
          HtmlUtil.renderCloseTag(w, "optgroup");
          currentGroup = null;
        }
        inGroup = false;
      }

      // Render Option
      List<Attribute> atts = new ArrayList<Attribute>();
      atts.add(new Attribute("value", option.getCode(), null, null));
      if (Objects.equals(option.getValue(), config.getDefaultValue())
          || Objects.equals(option.getCode(), config.getDefaultValue())) {
        atts.add(new Attribute("selected", "true", null, null));
      }
      HtmlUtil.renderOpenTag(w, "option", atts);
      w.write(HtmlUtil.escapeHtml(option.getLabel()));
      HtmlUtil.renderCloseTag(w, "option");
    }

    // Close Last Option Group if Appropriate
    if (inGroup) {
      HtmlUtil.renderCloseTag(w, "optgroup");
    }
    HtmlUtil.renderCloseTag(w, "select");
  }
}
