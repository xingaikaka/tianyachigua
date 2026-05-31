import React, { useEffect, useMemo, useRef } from 'react';
import advertisementService, {
  SHORT_VIDEO_AD_GRID_CAPACITY,
  SHORT_VIDEO_AD_TILE_SIZE,
  SHORT_VIDEO_AD_GRID_COLUMNS,
  SHORT_VIDEO_AD_GRID_ROWS
} from '../../services/advertisementService';
import adStatsTracker from '../../utils/adStatsTracker';

const PLACEHOLDER_IMAGE = '/logo_ad.png';
const TILE_GAP = 8;

const buildTiles = (ads) => {
  const normalized = Array.isArray(ads) ? ads.filter(Boolean) : [];
  const tiles = normalized.slice(0, SHORT_VIDEO_AD_GRID_CAPACITY);

  if (tiles.length < SHORT_VIDEO_AD_GRID_CAPACITY) {
    tiles.push(...Array(SHORT_VIDEO_AD_GRID_CAPACITY - tiles.length).fill(null));
  }

  return tiles;
};

const ShortVideoAdGrid = ({ ads = [], onAdClick, style, variant = 'default', aspectRatio = 'portrait' }) => {
  const containerRef = useRef(null);
  const tiles = useMemo(() => buildTiles(ads), [ads]);
  const isBare = variant === 'bare';
  const isLandscape = aspectRatio === 'landscape'; // 扁平模式

  useEffect(() => {
    const el = containerRef.current;
    if (!el) return undefined;

    const observer = new IntersectionObserver((entries) => {
      entries.forEach((entry) => {
        if (!entry.isIntersecting) return;

        tiles.forEach((tile) => {
          if (!tile || !tile.id) return;
          if (!adStatsTracker.startImpression(tile.id)) return;

          advertisementService.recordAdImpression(tile.id).catch(() => {
            adStatsTracker.cancelImpression(tile.id);
          });
        });
      });
    }, { threshold: 0.5 });

    observer.observe(el);
    return () => observer.disconnect();
  }, [tiles]);

  const handleTileClick = (tile) => {
    if (!tile || !tile.id) {
      return;
    }

    advertisementService.clickAd(tile.id).catch(() => { });

    if (onAdClick) {
      onAdClick(tile);
    }

    if (tile.linkUrl) {
      window.open(tile.linkUrl, '_blank');
    }
  };

  // 扁平模式下的网格配置（适配两个视频卡片宽度：520px + gap）
  // 调整为5列x3行布局，显示15个广告
  // 总宽度 = 5*62 + 4*8 = 310 + 32 = 342px
  // 总高度 = 3*62 + 2*8 + 2*8 = 186 + 16 + 16 = 218px
  const landscapeConfig = {
    columns: 5, // 5列
    tileSize: 62, // 62px图标，调小以匹配视频卡片高度
    rows: 3, // 3行
    capacity: 15 // 5x3=15个tile
  };

  // 竖屏模式下的网格配置（默认）
  const portraitConfig = {
    columns: SHORT_VIDEO_AD_GRID_COLUMNS, // 3列
    rows: SHORT_VIDEO_AD_GRID_ROWS,      // 5行
    tileSize: SHORT_VIDEO_AD_TILE_SIZE,  // 80px
    capacity: SHORT_VIDEO_AD_GRID_CAPACITY // 15个tile
  };

  const config = isLandscape ? landscapeConfig : portraitConfig;

  // 扁平模式下重新构建tiles
  const displayTiles = useMemo(() => {
    if (isLandscape) {
      const normalized = Array.isArray(ads) ? ads.filter(Boolean) : [];
      // 确保填满整行
      const totalSlots = config.capacity;
      const tiles = normalized.slice(0, totalSlots);
      if (tiles.length < totalSlots) {
        return [...tiles, ...Array(totalSlots - tiles.length).fill(null)];
      }
      return tiles;
    }
    return tiles;
  }, [ads, isLandscape, config.capacity, tiles]);

  const wrapperStyle = {
    display: 'flex',
    alignItems: isBare ? 'flex-start' : 'center',
    justifyContent: 'center',
    width: '100%',
    height: '100%',
    minHeight: isBare
      ? (isLandscape
        ? '100%' // 扁平模式：让容器自适应高度
        : `calc(var(--cell, 260px) * 16 / 9 + 60px)`) // 竖屏模式：9:16比例
      : undefined,
    boxSizing: 'border-box',
    background: isBare ? 'transparent' : '#000',
    padding: isBare
      ? (isLandscape
        ? '8px 0' // 扁平模式：减小垂直内边距，确保与视频卡片高度对齐
        : 0)
      : 16, // 非bare模式保持原有padding
    borderRadius: isBare ? 8 : 18, // 扁平模式使用与视频卡片相同的圆角
    ...style
  };

  const gridStyle = {
    display: 'grid',
    gridTemplateColumns: `repeat(${config.columns}, ${config.tileSize}px)`,
    gap: isLandscape ? '8px' : `${TILE_GAP}px`, // 调整间距
    justifyContent: 'center',
    alignContent: isBare ? 'flex-start' : 'center',
    width: isLandscape ? 'auto' : '100%', // 扁平模式自适应宽度
    margin: '0 auto' // 居中显示
  };

  // 扁平模式下添加包装类名，用于CSS样式控制
  const wrapperClassName = isBare && isLandscape ? 'paged-ad-wrapper' : '';

  return (
    <div ref={containerRef} style={wrapperStyle} className={wrapperClassName}>
      <div style={gridStyle}>
        {displayTiles.map((tile, index) => {
          const isPlaceholder = !tile || !tile.id;
          const key = tile?.id ? `ad-${tile.id}` : `placeholder-${index}`;

          return (
            <div
              key={key}
              onClick={() => handleTileClick(tile)}
              style={{
                width: `${config.tileSize}px`,
                height: `${config.tileSize}px`,
                borderRadius: 12,
                overflow: 'hidden',
                position: 'relative',
                cursor: isPlaceholder ? 'default' : 'pointer',
                background: isBare ? 'transparent' : '#1f1f1f',
                boxShadow: isBare ? 'none' : '0 4px 12px rgba(0, 0, 0, 0.25)'
              }}
            >
              <img
                src={tile?.imageUrl || PLACEHOLDER_IMAGE}
                alt={tile?.title || '广告位招租'}
                style={{
                  width: '100%',
                  height: '100%',
                  objectFit: 'cover'
                }}
              />
            </div>
          );
        })}
      </div>
    </div>
  );
};

export default ShortVideoAdGrid;
