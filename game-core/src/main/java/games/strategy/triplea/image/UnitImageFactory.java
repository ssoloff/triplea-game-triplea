package games.strategy.triplea.image;

import games.strategy.engine.data.PlayerId;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Constants;
import games.strategy.triplea.ResourceLoader;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.TechTracker;
import games.strategy.triplea.ui.mapdata.MapData;
import games.strategy.triplea.util.UnitCategory;
import games.strategy.ui.Util;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.swing.ImageIcon;

/** A factory for creating unit images. */
public class UnitImageFactory {
  public static final int DEFAULT_UNIT_ICON_SIZE = 48;
  /**
   * Width of all icons. You probably want getUnitImageWidth(), which takes scale factor into
   * account.
   */
  private static int unitIconWidth = DEFAULT_UNIT_ICON_SIZE;
  /**
   * Height of all icons. You probably want getUnitImageHeight(), which takes scale factor into
   * account.
   */
  private static int unitIconHeight = DEFAULT_UNIT_ICON_SIZE;

  private static int unitCounterOffsetWidth = DEFAULT_UNIT_ICON_SIZE / 4;
  private static int unitCounterOffsetHeight = unitIconHeight;
  private static final String FILE_NAME_BASE = "units/";
  // maps Point -> image
  private final Map<String, Image> images = new HashMap<>();
  // maps Point -> Icon
  private final Map<String, ImageIcon> icons = new HashMap<>();
  // Scaling factor for unit images
  private double scaleFactor;
  private ResourceLoader resourceLoader;
  private MapData mapData;

  public UnitImageFactory() {}

  public void setResourceLoader(
      final ResourceLoader loader,
      final double scaleFactor,
      final int initialUnitWidth,
      final int initialUnitHeight,
      final int initialUnitCounterOffsetWidth,
      final int initialUnitCounterOffsetHeight,
      final MapData mapData) {
    unitIconWidth = initialUnitWidth;
    unitIconHeight = initialUnitHeight;
    unitCounterOffsetWidth = initialUnitCounterOffsetWidth;
    unitCounterOffsetHeight = initialUnitCounterOffsetHeight;
    this.scaleFactor = scaleFactor;
    resourceLoader = loader;
    this.mapData = mapData;
    clearImageCache();
  }

  /** Set the unitScaling factor. */
  public void setScaleFactor(final double scaleFactor) {
    if (this.scaleFactor != scaleFactor) {
      this.scaleFactor = scaleFactor;
      clearImageCache();
    }
  }

  /** Return the unit scaling factor. */
  public double getScaleFactor() {
    return scaleFactor;
  }

  /** Return the width of scaled units. */
  public int getUnitImageWidth() {
    return (int) (scaleFactor * unitIconWidth);
  }

  /** Return the height of scaled units. */
  public int getUnitImageHeight() {
    return (int) (scaleFactor * unitIconHeight);
  }

  public int getUnitCounterOffsetWidth() {
    return (int) (scaleFactor * unitCounterOffsetWidth);
  }

  public int getUnitCounterOffsetHeight() {
    return (int) (scaleFactor * unitCounterOffsetHeight);
  }

  // Clear the image and icon cache
  private void clearImageCache() {
    images.clear();
    icons.clear();
  }

  public Image getImage(final UnitCategory unit) {
    return getImage(unit.getType(), unit.getOwner(), (unit.getDamaged() > 0), unit.getDisabled())
        .orElseThrow(() -> new RuntimeException("No unit image for: " + unit));
  }

  /** Return the appropriate unit image. */
  public Optional<Image> getImage(
      final UnitType type, final PlayerId player, final boolean damaged, final boolean disabled) {
    final String baseName = getBaseImageName(type, player, damaged, disabled);
    final String fullName = baseName + player.getName();
    if (images.containsKey(fullName)) {
      return Optional.of(images.get(fullName));
    }
    final Optional<Image> image = getTransformedImage(baseName, player, type);
    if (image.isEmpty()) {
      return Optional.empty();
    }
    final Image baseImage = image.get();

    // We want to scale units according to the given scale factor.
    // We use smooth scaling since the images are cached to allow to take our time in doing the
    // scaling.
    // Image observer is null, since the image should have been guaranteed to be loaded.
    final int width = (int) (baseImage.getWidth(null) * scaleFactor);
    final int height = (int) (baseImage.getHeight(null) * scaleFactor);
    final Image scaledImage = baseImage.getScaledInstance(width, height, Image.SCALE_SMOOTH);
    // Ensure the scaling is completed.
    Util.ensureImageLoaded(scaledImage);
    images.put(fullName, scaledImage);
    return Optional.of(scaledImage);
  }

  public Optional<URL> getBaseImageUrl(final String baseImageName, final PlayerId id) {
    return getBaseImageUrl(baseImageName, id, resourceLoader);
  }

  private static Optional<URL> getBaseImageUrl(
      final String baseImageName, final PlayerId id, final ResourceLoader resourceLoader) {
    // URL uses '/' not '\'
    final String fileName = FILE_NAME_BASE + id.getName() + "/" + baseImageName + ".png";
    final String fileName2 = FILE_NAME_BASE + baseImageName + ".png";
    final URL url = resourceLoader.getResource(fileName, fileName2);
    return Optional.ofNullable(url);
  }

  private Optional<Image> getTransformedImage(
      final String baseImageName, final PlayerId id, final UnitType type) {
    final Optional<URL> imageLocation = getBaseImageUrl(baseImageName, id);
    Image image = null;
    if (imageLocation.isPresent()) {
      image = Toolkit.getDefaultToolkit().getImage(getBaseImageUrl(baseImageName, id).get());
      Util.ensureImageLoaded(image);
      if (needToTransformImage(id, type, mapData)) {
        image = convertToBufferedImage(image);
        if (mapData.getUnitColor(id.getName()).isPresent()) {
          final Color color = mapData.getUnitColor(id.getName()).get();
          final int brightness = mapData.getUnitBrightness(id.getName());
          ImageTransformer.colorize(color, brightness, (BufferedImage) image);
        }
        if (mapData.shouldFlipUnit(id.getName())) {
          image = ImageTransformer.flipHorizontally((BufferedImage) image);
        }
      }
    }
    return Optional.ofNullable(image);
  }

  private static boolean needToTransformImage(
      final PlayerId id, final UnitType type, final MapData mapData) {
    return !mapData.ignoreTransformingUnit(type.getName())
        && (mapData.getUnitColor(id.getName()).isPresent() || mapData.shouldFlipUnit(id.getName()));
  }

  private static BufferedImage convertToBufferedImage(final Image image) {
    final BufferedImage newImage =
        new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_ARGB);
    final Graphics2D g = newImage.createGraphics();
    g.drawImage(image, 0, 0, null);
    g.dispose();
    return newImage;
  }

  /**
   * Returns the highlight image for the specified unit.
   *
   * @return The highlight image or empty if no base image is available for the specified unit.
   */
  public Optional<Image> getHighlightImage(
      final UnitType type, final PlayerId player, final boolean damaged, final boolean disabled) {
    return getImage(type, player, damaged, disabled).map(UnitImageFactory::highlightImage);
  }

  private static Image highlightImage(final Image image) {
    final BufferedImage highlightedImage =
        Util.newImage(image.getWidth(null), image.getHeight(null), true);
    // copy the real image
    final Graphics2D g = highlightedImage.createGraphics();
    g.drawImage(image, 0, 0, null);
    // we want a highlight only over the area that is not clear
    g.setComposite(AlphaComposite.SrcIn);
    g.setColor(new Color(240, 240, 240, 127));
    g.fillRect(0, 0, image.getWidth(null), image.getHeight(null));
    g.dispose();
    return highlightedImage;
  }

  /** Return a icon image for a unit. */
  public Optional<ImageIcon> getIcon(
      final UnitType type, final PlayerId player, final boolean damaged, final boolean disabled) {
    final String baseName = getBaseImageName(type, player, damaged, disabled);
    final String fullName = baseName + player.getName();
    if (icons.containsKey(fullName)) {
      return Optional.of(icons.get(fullName));
    }
    final Optional<Image> image = getTransformedImage(baseName, player, type);
    if (image.isEmpty()) {
      return Optional.empty();
    }

    final ImageIcon icon = new ImageIcon(image.get());
    icons.put(fullName, icon);
    return Optional.of(icon);
  }

  private static String getBaseImageName(
      final UnitType type, final PlayerId id, final boolean damaged, final boolean disabled) {
    StringBuilder name = new StringBuilder(32);
    name.append(type.getName());
    if (!type.getName().endsWith("_hit") && !type.getName().endsWith("_disabled")) {
      if (type.getName().equals(Constants.UNIT_TYPE_AAGUN)) {
        if (TechTracker.hasRocket(id) && UnitAttachment.get(type).getIsRocket()) {
          name = new StringBuilder("rockets");
        }
        if (TechTracker.hasAaRadar(id) && Matches.unitTypeIsAaForAnything().test(type)) {
          name.append("_r");
        }
      } else if (UnitAttachment.get(type).getIsRocket()
          && Matches.unitTypeIsAaForAnything().test(type)) {
        if (TechTracker.hasRocket(id)) {
          name.append("_rockets");
        }
        if (TechTracker.hasAaRadar(id)) {
          name.append("_r");
        }
      } else if (UnitAttachment.get(type).getIsRocket()) {
        if (TechTracker.hasRocket(id)) {
          name.append("_rockets");
        }
      } else if (Matches.unitTypeIsAaForAnything().test(type)) {
        if (TechTracker.hasAaRadar(id)) {
          name.append("_r");
        }
      }
      if (UnitAttachment.get(type).getIsAir() && !UnitAttachment.get(type).getIsStrategicBomber()) {
        if (TechTracker.hasLongRangeAir(id)) {
          name.append("_lr");
        }
        if (TechTracker.hasJetFighter(id)
            && (UnitAttachment.get(type).getAttack(id) > 0
                || UnitAttachment.get(type).getDefense(id) > 0)) {
          name.append("_jp");
        }
      }
      if (UnitAttachment.get(type).getIsAir() && UnitAttachment.get(type).getIsStrategicBomber()) {
        if (TechTracker.hasLongRangeAir(id)) {
          name.append("_lr");
        }
        if (TechTracker.hasHeavyBomber(id)) {
          name.append("_hb");
        }
      }
      if (UnitAttachment.get(type).getIsFirstStrike()
          && (UnitAttachment.get(type).getAttack(id) > 0
              || UnitAttachment.get(type).getDefense(id) > 0)) {
        if (TechTracker.hasSuperSubs(id)) {
          name.append("_ss");
        }
      }
      if (type.getName().equals(Constants.UNIT_TYPE_FACTORY)
          || UnitAttachment.get(type).getCanProduceUnits()) {
        if (TechTracker.hasIndustrialTechnology(id)
            || TechTracker.hasIncreasedFactoryProduction(id)) {
          name.append("_it");
        }
      }
    }
    if (disabled) {
      name.append("_disabled");
    } else if (damaged) {
      name.append("_hit");
    }
    return name.toString();
  }

  public Dimension getImageDimensions(final UnitType type, final PlayerId player) {
    final String baseName = getBaseImageName(type, player, false, false);
    final Image baseImage =
        getTransformedImage(baseName, player, type)
            .orElseThrow(
                () ->
                    new RuntimeException(
                        "No image for unit type: " + type + ", player: " + player));
    final int width = (int) (baseImage.getWidth(null) * scaleFactor);
    final int height = (int) (baseImage.getHeight(null) * scaleFactor);
    return new Dimension(width, height);
  }
}
