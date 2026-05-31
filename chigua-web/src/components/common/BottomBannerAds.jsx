import React from 'react';
import SecureDecryptedImage from '../common/SecureDecryptedImage';

const BottomBannerAds = ({
  ads = [],
  onAdClick,
  containerClassName = 'mb-6 md:mb-12',
  containerStyle = { backgroundColor: '#2C2A2A' },
  slotClassName = 'flex justify-center py-3 md:py-6 px-1 md:px-4',
  innerStyle = { maxWidth: '770px' },
  stackClassName = 'space-y-2 md:space-y-4',
  imageAlt = '底部横幅广告'
}) => {
  if (!ads || ads.length === 0) {
    return null;
  }

  return (
    <div className={containerClassName} style={containerStyle}>
      <div className={slotClassName}>
        <div className="w-full" style={innerStyle}>
          <div className={stackClassName}>
            {ads.map((ad, index) => (
              <div
                key={ad?.id || index}
                className="cursor-pointer overflow-hidden"
                onClick={() => onAdClick?.(ad)}
              >
                <div className="relative shadow-lg overflow-hidden">
                  <SecureDecryptedImage
                    src={ad?.imageUrl}
                    alt={imageAlt}
                    className="w-full h-auto object-contain md:h-[90px] md:object-cover md:max-h-[90px]"
                    style={{ width: '100%' }}
                    fallbackSrc="/800x100.jpg"
                    lazyLoad={false}
                    priority="normal"
                    onError={(e) => {
                      if (!e.target.src.endsWith('/800x100.jpg')) {
                        e.target.src = '/800x100.jpg';
                      }
                    }}
                  />
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
};

export default BottomBannerAds;
