<template>
  <div :class="classObj" class="app-wrapper" :style="{'--current-color': theme}">
    <div v-if="device==='mobile'&&sidebar.opened" class="drawer-bg" @click="handleClickOutside"/>
    <sidebar v-if="!sidebar.hide" class="sidebar-container"/>
    <div :class="{hasTagsView:needTagsView,sidebarHide:sidebar.hide}" class="main-container">
      <div :class="{'fixed-header':fixedHeader}">
        <navbar @setLayout="setLayout"/>
        <tags-view v-if="needTagsView"/>
      </div>
      <app-main/>
      <settings ref="settingRef"/>
    </div>
    <!-- 广告到期提醒组件 -->
    <ad-expiry-notification 
      :expiring-ads="expiringAds" 
      @close="handleCloseAdNotification"
    />
  </div>
</template>

<script>
import { AppMain, Navbar, Settings, Sidebar, TagsView } from './components'
import AdExpiryNotification from '@/components/AdExpiryNotification'
import ResizeMixin from './mixin/ResizeHandler'
import { mapState } from 'vuex'
import variables from '@/assets/styles/variables.scss'

export default {
  name: 'Layout',
  components: {
    AppMain,
    Navbar,
    Settings,
    Sidebar,
    TagsView,
    AdExpiryNotification
  },
  mixins: [ResizeMixin],
  data() {
    return {
      expiringAds: []
    }
  },
  mounted() {
    // 监听广告到期提醒事件
    console.log('Layout mounted - 添加广告到期事件监听器');
    window.addEventListener('showAdExpiryNotification', this.handleAdExpiryNotification)
  },
  beforeDestroy() {
    // 清理事件监听器
    window.removeEventListener('showAdExpiryNotification', this.handleAdExpiryNotification)
  },
  computed: {
    ...mapState({
      theme: state => state.settings.theme,
      sideTheme: state => state.settings.sideTheme,
      sidebar: state => state.app.sidebar,
      device: state => state.app.device,
      needTagsView: state => state.settings.tagsView,
      fixedHeader: state => state.settings.fixedHeader
    }),
    classObj() {
      return {
        hideSidebar: !this.sidebar.opened,
        openSidebar: this.sidebar.opened,
        withoutAnimation: this.sidebar.withoutAnimation,
        mobile: this.device === 'mobile'
      }
    },
    variables() {
      return variables
    }
  },
  methods: {
    handleClickOutside() {
      this.$store.dispatch('app/closeSideBar', { withoutAnimation: false })
    },
    setLayout() {
      this.$refs.settingRef.openSetting()
    },
    handleAdExpiryNotification(event) {
      // 处理广告到期提醒事件
      console.log('Layout收到广告到期提醒事件:', event.detail.expiringAds);
      this.expiringAds = event.detail.expiringAds || []
    },
    handleCloseAdNotification() {
      // 关闭广告到期提醒
      this.expiringAds = []
    }
  }
}
</script>

<style lang="scss" scoped>
  @import "~@/assets/styles/mixin.scss";
  @import "~@/assets/styles/variables.scss";

  .app-wrapper {
    @include clearfix;
    position: relative;
    height: 100%;
    width: 100%;

    &.mobile.openSidebar {
      position: fixed;
      top: 0;
    }
  }

  .drawer-bg {
    background: #000;
    opacity: 0.3;
    width: 100%;
    top: 0;
    height: 100%;
    position: absolute;
    z-index: 999;
  }

  .fixed-header {
    position: fixed;
    top: 0;
    right: 0;
    z-index: 9;
    width: calc(100% - #{$base-sidebar-width});
    transition: width 0.28s;
  }

  .hideSidebar .fixed-header {
    width: calc(100% - 54px);
  }

  .sidebarHide .fixed-header {
    width: 100%;
  }

  .mobile .fixed-header {
    width: 100%;
  }
</style>
