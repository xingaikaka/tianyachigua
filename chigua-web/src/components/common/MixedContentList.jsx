import React from 'react';

/**
 * 通用的内容/广告混排列表渲染组件。
 * - `items` 中 `type === 'ad'` 会走广告渲染函数，其余走内容渲染函数。
 * - 通过 `gapDesktop` / `gapMobile` 控制垂直间距，可结合 `flex flex-col` 使用。
 * - `getItemWrapperProps` 可为每一项追加自定义属性（如 id、额外 class）。
 */
const MixedContentList = ({
  items = [],
  renderContentItem,
  renderAdItem,
  isDesktop = false,
  gapDesktop,
  gapMobile,
  containerClassName,
  containerStyle,
  dataAttribute,
  itemWrapperClassName = 'flex justify-center',
  getItemKey,
  getItemWrapperProps
}) => {
  let contentIndex = -1;

  const containerProps = {
    className: containerClassName,
    style: containerStyle ? { ...containerStyle } : undefined
  };

  if (dataAttribute) {
    containerProps[dataAttribute] = true;
  }

  if (!containerProps.style && gapDesktop != null && gapMobile != null) {
    containerProps.style = {
      gap: isDesktop ? gapDesktop : gapMobile
    };
  } else if (containerProps.style && gapDesktop != null && gapMobile != null) {
    containerProps.style = {
      ...containerProps.style,
      gap: isDesktop ? gapDesktop : gapMobile
    };
  }

  return (
    <div {...containerProps}>
      {items.map((item, idx) => {
        const isAd = item?.type === 'ad';
        const currentContentIndex = isAd ? contentIndex : contentIndex + 1;
        if (!isAd) {
          contentIndex = currentContentIndex;
        }

        const key = getItemKey
          ? getItemKey(item, idx, currentContentIndex)
          : `${item?.type || 'item'}-${item?.id ?? item?.data?.id ?? idx}`;

        const wrapperProps = getItemWrapperProps
          ? (getItemWrapperProps(item, idx, currentContentIndex) || {})
          : {};

        const combinedClassName = [
          itemWrapperClassName,
          wrapperProps.className
        ]
          .filter(Boolean)
          .join(' ')
          .trim() || undefined;

        if (combinedClassName) {
          wrapperProps.className = combinedClassName;
        } else {
          delete wrapperProps.className;
        }

        const content = isAd
          ? renderAdItem?.(item, idx)
          : renderContentItem?.(item, currentContentIndex, idx);

        return (
          <div key={key} {...wrapperProps}>
            {content}
          </div>
        );
      })}
    </div>
  );
};

export default MixedContentList;
