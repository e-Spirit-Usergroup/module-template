package com.espirit.modules.to_be_renamed.urlfactory;

import com.espirit.moddev.components.annotations.UrlFactoryComponent;
import de.espirit.common.io.IoError;
import de.espirit.common.tools.Strings;
import de.espirit.firstspirit.access.Language;
import de.espirit.firstspirit.access.project.Project;
import de.espirit.firstspirit.access.project.Resolution;
import de.espirit.firstspirit.access.project.TemplateSet;
import de.espirit.firstspirit.access.store.ContentProducer;
import de.espirit.firstspirit.access.store.IDProvider;
import de.espirit.firstspirit.access.store.LanguageInfo;
import de.espirit.firstspirit.access.store.PageParams;
import de.espirit.firstspirit.access.store.mediastore.File;
import de.espirit.firstspirit.access.store.mediastore.Media;
import de.espirit.firstspirit.access.store.mediastore.MediaMetaData;
import de.espirit.firstspirit.access.store.mediastore.Picture;
import de.espirit.firstspirit.access.store.sitestore.Content2Params;
import de.espirit.firstspirit.access.store.sitestore.ContentMultiPageParams.ContentPageParams;
import de.espirit.firstspirit.access.store.sitestore.PageRef;
import de.espirit.firstspirit.access.store.sitestore.SiteStoreFolder;
import de.espirit.firstspirit.generate.PathLookup;
import de.espirit.firstspirit.generate.UrlFactory;
import de.espirit.or.schema.Entity;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;


/**
 * Search engine-optimized path factory. The base name of a node is produced from the language dependent display name
 *  * (see {@link #getName(IDProvider, Language)}).
 */
@UrlFactoryComponent(name = "To_be_renamedUrlFactory")
public class To_be_renamedUrlFactory implements UrlFactory {

  protected enum WelcomeFileMode {DISABLED, FOR_FIRST_HTML_CHANNEL, FOR_ALL_HTML_CHANNELS, FOR_SELECTED_CHANNELS}

  protected static final String USELOWERCASE = "uselowercase";
  protected static final String USEWELCOMEFILENAMES = "usewelcomefilenames";

  private PathLookup _pathLookup;
  private WelcomeFileMode _welcomeFileMode;
  private boolean _lowerCase;
  private Set<String> _channelsWithWelcomeFiles;


  /**
   * Initialize fields based on various settings and a {@link PathLookup} object.
   * @param settings Settings provided in module.xml file in section {@code <configuration>..</configuration>}.
   * The key is the tag name (converted to lower case), value is the text child node. E.g. {@code <key>value</key>}.
   * @param pathLookup Path lookup for user defined paths.
   */
  @Override
  public void init(@NotNull final Map<String, String> settings, @NotNull final PathLookup pathLookup) {
    _pathLookup = pathLookup;
    final String useWelcomeFilenames = settings.get(USEWELCOMEFILENAMES);
    if (booleanValue(useWelcomeFilenames, true)) {
      _welcomeFileMode = WelcomeFileMode.FOR_FIRST_HTML_CHANNEL;
    } else if ("all".equalsIgnoreCase(useWelcomeFilenames)) {
      _welcomeFileMode = WelcomeFileMode.FOR_ALL_HTML_CHANNELS;
    } else if (!falseOrUnset(useWelcomeFilenames)) {
      _welcomeFileMode = WelcomeFileMode.FOR_SELECTED_CHANNELS;
      _channelsWithWelcomeFiles = getChannelsWithWelcomeFiles(useWelcomeFilenames);
    } else {
      _welcomeFileMode = WelcomeFileMode.DISABLED;
    }
    _lowerCase = booleanValue(settings.get(USELOWERCASE), false);
  }

  private Set<String> getChannelsWithWelcomeFiles(final String useWelcomFilenames) {
    final Set<String> channelsWithWelcomeFiles = new HashSet<>();
    final String[] channels = useWelcomFilenames.split(",");
    for (final String channel : channels) {
      channelsWithWelcomeFiles.add(channel.trim());
    }
    return channelsWithWelcomeFiles;
  }


  private boolean booleanValue(final String parameter, final boolean defaultValue) {
    if (parameter == null) {
      return defaultValue;
    }
    return "yes".equalsIgnoreCase(parameter) || "true".equalsIgnoreCase(parameter);
  }


  private boolean falseOrUnset(final String parameter) {
    return parameter.isEmpty() || "no".equalsIgnoreCase(parameter) || "false".equalsIgnoreCase(parameter);
  }


  protected final WelcomeFileMode getWelcomeFileMode() {
    return _welcomeFileMode;
  }


  protected final boolean isLowerCaseMode() {
    return _lowerCase;
  }


  protected final PathLookup getPathLookup() {
    return _pathLookup;
  }


  /**
   * Build the URL for a content-producing store element.
   * @param contentProducer A store element.
   * @param templateSet The target template set.
   * @param language The target language.
   * @param pageParams Page parameters, used for content projection, etc.
   * @return The URL for the {@code contentProducer}, based on target template, target language and optional page parameters.
   */
  @Override
  @NotNull
  public String getUrl(@NotNull final ContentProducer contentProducer, @NotNull final TemplateSet templateSet, @NotNull final Language language, @NotNull final PageParams pageParams) {
    final String name = getName(contentProducer, templateSet, language, pageParams);
    String extension = contentProducer.getExtension(templateSet);
    int len = name.length();
    if (!extension.isEmpty()) {
      extension = adjustCase(extension);
      len += extension.length();
      len++; // for dot
    }
    final StringBuilder buffer = new StringBuilder(0);
    collectPath(getParentNotNull(contentProducer), language, templateSet, len, buffer);
    buffer.append(name);
    if (!extension.isEmpty()) {
      buffer.append('.');
      buffer.append(extension);
    }
    return buffer.toString();
  }


  /**
   * Build the URL for a Media Store element.
   * @param node The target node, located in the Media Store.
   * @param language Target language or {@code null} for language-independent media nodes.
   * @param resolution Target resolution or {@code null} for media nodes of type {@link de.espirit.firstspirit.access.store.mediastore.Media#FILE}.
   * @return The URL for the {@code node}, based on target language and optional resolution.
   */
  @Override
  @NotNull
  public String getUrl(@NotNull final Media node, @NotNull final Language language, @Nullable final Resolution resolution) {
    String name = getName(node, language);
    name = adjustCase(name);
    String resolutionString = null;
    int len = name.length();
    if ((resolution != null) && (node.getType() == Media.PICTURE) && !resolution.isOriginal()) {
      resolutionString = adjustCase(resolution.getUid());
      len += resolutionString.length();
      len++; // for underscore
    }
    String extension = getExtension(node, language, resolution);
    if (extension != null) {
      extension = adjustCase(extension);
      len += extension.length();
      len++; // for dot
    }
    final StringBuilder buffer = new StringBuilder(0);
    final IDProvider parent = getParentNotNull(node);
    collectPath(parent, language, null, len, buffer);
    buffer.append(name);
    if (resolutionString != null) {
      buffer.append('_');
      buffer.append(resolutionString);
    }
    if (extension != null) {
      buffer.append('.');
      buffer.append(extension);
    }
    return buffer.toString();
  }


  /**
   * Build a name for the provided node.
   *
   * This implementation first attempts to identify if the node is a page reference that is used for content projection and displays only a single
   * dataset; if so, the sitemap variable is used to form a name.
   *
   * If the node is not used for content projection, but the following two criteria are met:
   *  - the node is the start node of a Site Store folder
   *  - the configuration parameter "usewelcomefilenames" evaluates to "yes" or "true" (see {@code init(...)}
   * the node is named "index".
   *
   * If none of the above criteria match, the language-dependent display name of the node is retrieved and used as the node's name in the URL that
   * is being built.
   *
   * In all cases, the node names returned are processed using the {@code cleanup(...)} method.
   * @param contentProducer The node to identify a URL name for.
   * @param templateSet The template set for which to generate a URL name.
   * @param language The project language for which to generate a URL name.
   * @param pageParams Page parameters that may indicate if content projection is used.
   * @return The name of this {@code contentProducer}, to be used in forming a URL for this element.
   */
  private String getName(final ContentProducer contentProducer, final TemplateSet templateSet, final Language language, final PageParams pageParams) {
    if ((contentProducer instanceof PageRef) && (pageParams instanceof ContentPageParams) && (pageParams.getSize() == 1)) {
      final Content2Params content2Params = ((PageRef) contentProducer).getContent2Params();
      if (content2Params != null) {
        String varName = content2Params.getSitemapVariableName();
        if (varName != null) {
          if (varName.endsWith("*")) {
            varName = varName.substring(0, varName.length() - 1);
          }
          final ContentPageParams contentPageParams = (ContentPageParams) pageParams;
          final List<Entity> list = contentPageParams.getData();
          if (!list.isEmpty()) {
            final Entity entity = list.get(0);
            String result = resolve(contentProducer, entity, varName, language);
            if ( ! Strings.isEmpty(result)) {
              result = adjustCase(result);
              return cleanup(result, language);
            }
          }
        }
      }
    }
    if ((getWelcomeFileMode() != WelcomeFileMode.DISABLED) && (pageParams.getIndex() == 0) && !(pageParams instanceof ContentPageParams)) {
      final SiteStoreFolder folder = (SiteStoreFolder) contentProducer.getParent();
      if ((folder != null) && contentProducer.equals(folder.getStartNode()) && createIndexFile(templateSet, contentProducer.getProject())) {
        return "index";
      }
    }
    String name = getName(contentProducer, language);
    final String pageSuffix = pageParams.getPageSuffix();
    if (!pageSuffix.isEmpty()) {
      name += '_' + pageSuffix;
    }
    return adjustCase(name);
  }


  /**
   * Get the name part for the specified target node and entity. This implementation tries to resolve the given
   * {@code varName} for the given {@code entity}.
   *
   * @param contentProducer The target node.
   * @param entity          The target entity which is rendered.
   * @param varName         See {@link Content2Params#getSitemapVariableName()}.
   * @param language        The target language which.
   * @return A name or {@code null} if a generic name should be used.
   */
  @Nullable
  @SuppressWarnings("UnusedParameters")
  protected String resolve(final ContentProducer contentProducer, Entity entity, final String varName, final Language language) {
    String attribute = varName;
    final String[] attributes = varName.split("\\.");
    if (attributes.length > 1) {
      final int lastIndex = attributes.length - 1;
      for (int i = 0; i < lastIndex; i++) {
        final Object value = entity.getValue(attributes[i]);
        if (value instanceof Entity) {
          entity = (Entity) value;
        } else {
          return null;
        }
      }
      attribute = attributes[lastIndex];
    }
    if (entity.getEntityType().getAttribute(attribute) == null) {
      attribute = attribute + '_' + language.getAbbreviation();
    }
    final Object value = entity.getValue(attribute);
    if (value == null || "".equals(value)) {
      return null;
    }
    return value.toString();
  }


  private boolean createIndexFile(final TemplateSet templateSet, final Project project) {
    if (getWelcomeFileMode() == WelcomeFileMode.FOR_ALL_HTML_CHANNELS) {
      return true;
    }
    if (getWelcomeFileMode() == WelcomeFileMode.FOR_SELECTED_CHANNELS) {
      return requireNonNull(_channelsWithWelcomeFiles, "ChannelsWithWelcomeFiles not initialized")
          .contains(templateSet.getUid());  // _channelsWithWelcomeFiles must be initialized in this case
    }
    // assumption: we have WelcomeFileMode.FOR_FIRST_HTML_CHANNEL
    for (final TemplateSet set : project.getTemplateSets()) {
      if ("html".equals(set.getExtension())) {
        return templateSet.equals(set);
      }
    }
    return false; // should not be reached
  }


  /**
   * Build a base name for the provided node. This implementation takes the language dependent display name (see
   * {@link de.espirit.firstspirit.access.store.IDProvider#getLanguageInfo(Language)}). If this is not set for the
   * provided language the
   * {@link de.espirit.firstspirit.access.project.Project#getMasterLanguage() project master language} is used. If
   * this is also not set, the {@link de.espirit.firstspirit.access.store.IDProvider#getUid() uid of the node } is
   * used. Then leading and trailing chars are stripped and some chars with special meaning in URLs and file names are
   * replaced by '-' (see {@link #cleanup(String)}).
   *
   * @param node Get the name for this node.
   * @param language Get the name for this language.
   * @return Name part of path for provided node and language.
   */
  private String getName(@NotNull final IDProvider node, @NotNull final Language language) {
    LanguageInfo languageInfo = node.getLanguageInfo(language);
    String displayName = languageInfo != null ? languageInfo.getDisplayName() : null;
    if (displayName != null) {
      final String cleaned = cleanup(displayName, language);
      if ( ! cleaned.isEmpty()) {
        return cleaned;
      }
    }
    final Language masterLanguage = node.getProject().getMasterLanguage();
    if (!masterLanguage.equals(language)) {
      languageInfo = node.getLanguageInfo(masterLanguage);
      if (languageInfo != null) {
        displayName = languageInfo.getDisplayName();
        if (displayName != null) {
          final String cleaned = cleanup(displayName, language);
          if ( ! cleaned.isEmpty()) {
            return cleaned;
          }
        }
      }
    }
    return cleanup(node.getUid(), language);
  }


  /**
   * Recursive method building a slash-delimited path for the provided folder and its parent chain. For each folder on the
   * chain this methods calls {@link #getName(de.espirit.firstspirit.access.store.IDProvider,de.espirit.firstspirit.access.Language) getName(folder, language)}. For the root folder
   * (<tt>{@link IDProvider#getParent() folder.getParent()} == null</tt>) the constructed path is empty. The
   * constructed path will be appended to the provided {@link StringBuilder}. The constructed path will start and end
   * with a slash.
   *
   * @param folder folder for which the path from root is collected.
   * @param language language, will be forwarded to {@link #getName(IDProvider,Language)} to build the name of each
   * path element
   * @param templateSet template set, piped through to {@link PathLookup#lookupPath(IDProvider, Language, TemplateSet)}
   * - may be {@code null}.
   * @param length size estimation for the StringBuilder, will be increased in every call, used to
   * {@link StringBuilder#ensureCapacity(int) ensure its capacity} to prevent frequent resizing
   * @param collector the builded path is appended to this instance
   */
  final void collectPath(final IDProvider folder, final Language language, @Nullable final TemplateSet templateSet, final int length, final StringBuilder collector) {
    String name = getPathLookup().lookupPath(folder, language, templateSet);
    if (name == null) {
      name = getName(folder, language);
      name = adjustCase(name);
      final IDProvider parentFolder = folder.getParent();
      if (parentFolder == null) {
        throw new IllegalStateException();
      }
      collectPath(parentFolder, language, templateSet, length + name.length() + 1, collector);
      collector.append(name);
      collector.append('/');
    } else if ("/".equals(name)) {
      collector.ensureCapacity(length + 1);
      collector.append('/');
    } else {
      final int len = name.length();
      if (len > 0) {
        collector.ensureCapacity(length + len + 1);
        collector.append(name);
      } else {
        collector.ensureCapacity(length + 1);
      }
      collector.append('/');
    }
  }


  /**
   * Helper to determine the extension (e.g. "gif", "pdf") for the given media in the given {@code language} and
   * {@code resolution}.<p />
   * <p>
   * Provided language may be {@code null} for language independent media objects, resolution is null media objects
   * of type {@link Media#FILE}.
   *
   * @param media Media node to get the extension for.
   * @param lang Language to get the extension for (is {@code  null} if provided media not isn't langage dependent).
   * @param resolution Resolution to get the extension for (is {@code  null} if provided media is of type {@link Media#FILE}).
   * @return File extension.
   */
  @Nullable
  final String getExtension(final Media media, @Nullable final Language lang, @Nullable final Resolution resolution) {
    if (media.getType() == Media.FILE) {
      final File file = media.getFile(lang);
      if (file == null) {
        throw new RuntimeException("no file data found for media:\"" + media.getUid() + "\" (id=" + media.getId() + ')');
      }
      return file.getExtension();
    } else {
      if (resolution == null) {
        throw new NullPointerException();
      }
      try {
        final Picture picture = media.getPicture(lang);
        if (picture == null) {
          throw new RuntimeException("no picture data found for media:\"" + media.getUid() + "\" (id=" + media.getId() + ')');
        }
        final MediaMetaData mediaMetaData = picture.getPictureMetaData(resolution);
        if (mediaMetaData == null) {
          throw new RuntimeException("no picture data found for media:\"" + media.getUid() + "\" (id=" + media.getId() + ')');
        }
        return mediaMetaData.getExtension();
      } catch (final IOException e) {
        throw new IoError(e);
      }
    }
  }


  /**
   * Matcher for chars (with special meaning in URIs or not valid in windows file names):<ul>
   *     <li>;</li>
   *     <li>@</li>
   *     <li>&amp;</li>
   *     <li>=</li>
   *     <li>+</li>
   *     <li>$</li>
   *     <li>,</li>
   *     <li>/</li>
   *     <li>\</li>
   *     <li>&lt;</li>
   *     <li>&gt;</li>
   *     <li>:</li>
   *     <li>*</li>
   *     <li>|</li>
   *     <li>#</li>
   *     <li>?</li>
   *     <li>"</li>
   * <li>%</li>
   *     <li>whitespace</li>
   * </ul>
   */
  private static final Pattern SPECIAL_CHARS = Pattern.compile("(;|@|&|=|\\+|\\$|,|/|\\\\|<|>|:|\\*|\\||#|\\?|\"|\\s|%|-)+");


  /**
   * Strips leading and trailing whitespaces and replaces whitespaces and chars with a special meaning in URIs
   * or file names (e.g. under Windows(TM)) with a single minus character.
   * Also shortens names to 255 characters, and removes possible trailing dots.
   *
   * @param name String to clean up.
   * @param language Language for which the name was build, currently not used.
   * @return Cleaned string.
   */
  @SuppressWarnings("UnusedParameters")
  protected String cleanup(@NotNull String name, @NotNull final Language language) {
    name = name.trim();
    final Matcher matcher = SPECIAL_CHARS.matcher(name);
    if (matcher.find()) {
      String cleaned = matcher.replaceAll("-");
      if (cleaned.length() == 1) {
        return cleaned;
      }
      if (cleaned.charAt(0) == '-') {
        cleaned = cleaned.substring(1);
      }
      final int length = cleaned.length();
      if (cleaned.charAt(length - 1) == '-') {
        cleaned = cleaned.substring(0, length - 1);
      }
      name = cleaned;
    }
    if (name.length() > 255) {
      name = name.substring(0, 255);
    }
    while (name.endsWith(".")) {
      name = name.substring(0, name.length() - 1);
    }
    return name;
  }

  @NotNull
  private static IDProvider getParentNotNull(final IDProvider node) {
    final IDProvider parent = node.getParent();
    if (parent == null) {
      throw new IllegalStateException(node.getElementType() + ", id=" + node.getId() + " has no parent");
    }
    return parent;
  }


  private String adjustCase(final String value) {
    if (isLowerCaseMode()) {
      return value.toLowerCase(Locale.ENGLISH);
    }
    return value;
  }

}