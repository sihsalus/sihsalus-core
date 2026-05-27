package org.openmrs.module.htmlwidgets.web.html;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.openmrs.util.OpenmrsConstants;

/** This represents utility methods for writing tags */
public class HtmlUtil {

  public static final List<String> STANDARD_ATTRIBUTES =
      Arrays.asList(
          "id", "class", "style", "dir", "lang", "accesskey", "tabindex", "title", "xml:lang");

  public static final List<String> INPUT_ATTRIBUTES =
      Arrays.asList(
          "accept",
          "alt",
          "checked",
          "disabled",
          "maxlength",
          "name",
          "readonly",
          "size",
          "src",
          "type",
          "value");

  public static final List<String> OPTION_ATTRIBUTES =
      Arrays.asList("disabled", "label", "value", "selected");

  public static final List<String> OPTGROUP_ATTRIBUTES = Arrays.asList("disabled", "label");

  public static final List<String> A_ATTRIBUTES = Arrays.asList("href", "target", "rel");

  public static final List<String> SELECT_ATTRIBUTES =
      Arrays.asList("disabled", "multiple", "name", "size");

  public static final List<String> TEXTAREA_ATTRIBUTES =
      Arrays.asList("cols", "rows", "disabled", "name", "readonly");

  public static final List<String> STANDARD_EVENTS =
      Arrays.asList(
          "onblur",
          "onchange",
          "onclick",
          "ondblclick",
          "onfocus",
          "onmousedown",
          "onmousemove",
          "onmouseout",
          "onmouseover",
          "onmouseup",
          "onkeydown",
          "onkeypress",
          "onkeyup",
          "onselect");

  public static final List<String> JS_EXTENSIONS = Arrays.asList("js", "javascript", "jscript");

  private static final List<String> CSS_EXTENSIONS = Arrays.asList("css", "style", "stylesheet");

  private static final String INIT_REQ_UNIQUE_ID = "__INIT_REQ_UNIQUE_ID__";

  private static final String OPENMRS_HTML_INCLUDE_REQUEST_ID_KEY =
      "org.openmrs.htmlInclude.pageName";

  private static final String OPENMRS_HTML_INCLUDE_MAP_KEY = "org.openmrs.htmlInclude.includeMap";

  /**
   * Returns true if the passed attribute is valid for the passed tagName
   *
   * @param tagName the tagName to check
   * @param attribute the attribute to check
   * @return true if the passed attribute is valid for the passed tagName
   */
  public static boolean isValidTagAttribute(String tagName, String attribute) {
    if (StringUtils.isBlank(tagName) || StringUtils.isBlank(attribute)) {
      return false;
    }

    String tag = tagName.toLowerCase().trim();
    String att = attribute.toLowerCase().trim();
    if (STANDARD_ATTRIBUTES.contains(att) || STANDARD_EVENTS.contains(att)) {
      return true;
    }
    if ("input".equals(tag)) {
      return INPUT_ATTRIBUTES.contains(att);
    }
    if ("a".equals(tag)) {
      return A_ATTRIBUTES.contains(att);
    }
    if ("option".equals(tag)) {
      return OPTION_ATTRIBUTES.contains(att);
    }
    if ("optgroup".equals(tag)) {
      return OPTGROUP_ATTRIBUTES.contains(att);
    }
    if ("select".equals(tag)) {
      return SELECT_ATTRIBUTES.contains(att);
    }
    if ("textarea".equals(tag)) {
      return TEXTAREA_ATTRIBUTES.contains(att);
    }
    return false;
  }

  /**
   * Render the specified resource in the page
   *
   * @param w The writer to write the rendered resource tag to
   * @param request the active request
   * @param resource the resource path to render
   * @throws IOException
   */
  @SuppressWarnings("unchecked")
  public static void renderResource(Writer w, HttpServletRequest request, String resource)
      throws IOException {
    Map<String, String> rewrites = getRewritesFromHtmlIncludeTag();
    if (rewrites != null && rewrites.containsKey(resource)) resource = rewrites.get(resource);

    String ext = resource.substring(resource.lastIndexOf(".") + 1).toLowerCase();
    boolean isJs = JS_EXTENSIONS.contains(ext);
    boolean isCss = CSS_EXTENSIONS.contains(ext);
    HttpSession session = request.getSession();

    if (isJs || isCss) {
      String initialRequestId = (String) request.getAttribute(INIT_REQ_UNIQUE_ID);
      if (initialRequestId == null) {
        initialRequestId = String.valueOf(System.identityHashCode(request));
      }
      String lastRequestId = (String) session.getAttribute(OPENMRS_HTML_INCLUDE_REQUEST_ID_KEY);
      Map<String, String> m =
          (Map<String, String>) session.getAttribute(OPENMRS_HTML_INCLUDE_MAP_KEY);
      if (m == null || !initialRequestId.equals(lastRequestId)) {
        m = new HashMap<String, String>();
      }

      String prefix = request.getContextPath();
      String otherResource = prefix + resource;
      if (!m.containsKey(resource) && !m.containsKey(otherResource)) {
        m.put(resource, "true");
        session.setAttribute(OPENMRS_HTML_INCLUDE_MAP_KEY, m);
        session.setAttribute(OPENMRS_HTML_INCLUDE_REQUEST_ID_KEY, initialRequestId);

        if (!resource.startsWith(prefix + "/")) {
          resource = prefix + resource;
        }
        resource += "?v=" + OpenmrsConstants.OPENMRS_VERSION_SHORT;

        if (isJs) {
          w.write(
              "<script src=\""
                  + escapeAttribute(resource)
                  + "\" type=\"text/javascript\" ></script>");
        } else if (isCss) {
          w.write(
              "<link href=\""
                  + escapeAttribute(resource)
                  + "\" type=\"text/css\" rel=\"stylesheet\" />");
        }
      }
    }
  }

  /**
   * Since OpenMRS 1.7, certain resources are silently rewritten. This method tries to get the
   * rewrite map using reflection, so that we don't have to branch the htmlwidgets module into pre-
   * and post- OpenMRS 1.7 versions.
   */
  private static Map<String, String> getRewritesFromHtmlIncludeTag() {
    return null;
  }

  /**
   * Render each specified resource as an HtmlInclude in the page
   *
   * @param w
   * @param request
   * @param resources
   * @throws IOException
   */
  public static void renderResourceFiles(
      Writer w, HttpServletRequest request, List<String> resources) throws IOException {
    if (resources != null) {
      for (String s : resources) {
        renderResource(w, request, s);
      }
    }
  }

  /**
   * Render a simple tag that has no body
   *
   * @param w
   * @param tagName
   * @param attributes
   * @throws IOException
   */
  public static void renderSimpleTag(Writer w, String tagName, Collection<Attribute> attributes)
      throws IOException {
    w.write("<" + tagName);
    renderTagAttributes(w, tagName, attributes);
    w.write("/>");
  }

  /**
   * Render a simple tag that has no body
   *
   * @param w
   * @param tagName
   * @param attributeString
   * @throws IOException
   */
  public static void renderSimpleTag(Writer w, String tagName, String attributeString)
      throws IOException {
    w.write("<" + tagName);
    renderTagAttributes(w, tagName, attributeString);
    w.write("/>");
  }

  /**
   * Render an opening tag
   *
   * @param w
   * @param tagName
   * @param attributes
   * @throws IOException
   */
  public static void renderOpenTag(Writer w, String tagName, Collection<Attribute> attributes)
      throws IOException {
    w.write("<" + tagName);
    renderTagAttributes(w, tagName, attributes);
    w.write(">");
  }

  /**
   * Render an opening tag
   *
   * @param w
   * @param tagName
   * @param attributeString
   * @throws IOException
   */
  public static void renderOpenTag(Writer w, String tagName, String attributeString)
      throws IOException {
    w.write("<" + tagName);
    renderTagAttributes(w, tagName, attributeString);
    w.write(">");
  }

  /**
   * Render a closing tag
   *
   * @param w
   * @param tagName
   * @throws IOException
   */
  public static void renderCloseTag(Writer w, String tagName) throws IOException {
    w.write("</" + tagName + ">");
  }

  /**
   * Render the attribute map as it should be output in a tag
   *
   * @param w
   * @param tagName
   * @param attributes
   * @throws IOException
   */
  public static void renderTagAttributes(Writer w, String tagName, Collection<Attribute> attributes)
      throws IOException {
    if (attributes != null) {
      for (Attribute a : attributes) {
        String attributeName = a.getName() == null ? null : a.getName().toLowerCase().trim();
        if (isValidTagAttribute(tagName, attributeName)) {
          if (StringUtils.isNotEmpty(a.getValue())
              || ("value".equals(attributeName) && a.getValue().isEmpty())) {
            w.write(" " + attributeName + "=\"" + escapeAttribute(a.getValue()) + "\"");
          }
        }
      }
    }
  }

  /**
   * Render the attribute map as it should be output in a tag
   *
   * @param w
   * @param tagName
   * @param attributeString
   * @throws IOException
   */
  public static void renderTagAttributes(Writer w, String tagName, String attributeString)
      throws IOException {
    if (StringUtils.isNotEmpty(attributeString)) {
      for (String attribute : attributeString.split("\\|")) {
        String[] nameVal = attribute.split("=", 2);
        if (nameVal.length != 2) {
          throw new IllegalArgumentException(
              "Misformed argument in attributeString: <" + attributeString + ">");
        }
        String attributeName = nameVal[0].toLowerCase().trim();
        if (isValidTagAttribute(tagName, attributeName)) {
          if (StringUtils.isNotEmpty(nameVal[1])
              || ("value".equals(attributeName) && nameVal[1].isEmpty())) {
            w.write(" " + attributeName + "=\"" + escapeAttribute(nameVal[1]) + "\"");
          }
        }
      }
    }
  }

  public static String escapeHtml(Object value) {
    if (value == null) {
      return "";
    }

    String input = value.toString();
    StringBuilder escaped = new StringBuilder(input.length());
    for (int i = 0; i < input.length(); i++) {
      char c = input.charAt(i);
      switch (c) {
        case '&':
          escaped.append("&amp;");
          break;
        case '<':
          escaped.append("&lt;");
          break;
        case '>':
          escaped.append("&gt;");
          break;
        case '"':
          escaped.append("&quot;");
          break;
        case '\'':
          escaped.append("&#39;");
          break;
        default:
          escaped.append(c);
      }
    }
    return escaped.toString();
  }

  public static String escapeAttribute(Object value) {
    return escapeHtml(value);
  }

  public static String escapeJavaScriptString(Object value) {
    if (value == null) {
      return "";
    }

    String input = value.toString();
    StringBuilder escaped = new StringBuilder(input.length());
    for (int i = 0; i < input.length(); i++) {
      char c = input.charAt(i);
      switch (c) {
        case '\\':
          escaped.append("\\\\");
          break;
        case '"':
          escaped.append("\\\"");
          break;
        case '\'':
          escaped.append("\\'");
          break;
        case '\n':
          escaped.append("\\n");
          break;
        case '\r':
          escaped.append("\\r");
          break;
        case '\t':
          escaped.append("\\t");
          break;
        case '\b':
          escaped.append("\\b");
          break;
        case '\f':
          escaped.append("\\f");
          break;
        case '<':
          escaped.append("\\u003C");
          break;
        case '>':
          escaped.append("\\u003E");
          break;
        case '&':
          escaped.append("\\u0026");
          break;
        case '\u2028':
          escaped.append("\\u2028");
          break;
        case '\u2029':
          escaped.append("\\u2029");
          break;
        default:
          if (c < 0x20) {
            appendUnicodeEscape(escaped, c);
          } else {
            escaped.append(c);
          }
      }
    }
    return escaped.toString();
  }

  private static void appendUnicodeEscape(StringBuilder escaped, char c) {
    escaped.append("\\u");
    String hex = Integer.toHexString(c);
    for (int i = hex.length(); i < 4; i++) {
      escaped.append('0');
    }
    escaped.append(hex);
  }
}
