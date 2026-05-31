/**
 * 分类缓存功能测试工具
 * 用于验证缓存机制是否正常工作
 */

import categoryCacheService from '../services/categoryCacheService';

class CategoryCacheTest {
  constructor() {
    this.testResults = [];
  }

  /**
   * 运行所有测试
   */
  async runAllTests() {
    try {
      await this.testCacheService();
      await this.testDataConsistency();
      await this.testErrorHandling();

      return this.printResults();
    } catch (error) {
      return {
        summary: '执行测试失败',
        passed: 0,
        total: 0,
        details: [{ name: '运行异常', passed: false, details: error.message }]
      };
    }
  }

  /**
   * 测试缓存服务基本功能
   */
  async testCacheService() {
    
    try {
      // 清理缓存
      categoryCacheService.clearCache();
      
      // 第一次获取数据（应该从API获取）
      const startTime1 = Date.now();
      const categories1 = await categoryCacheService.getCategories();
      const time1 = Date.now() - startTime1;
      
      this.addResult('首次获取分类数据', categories1.length > 0, `耗时: ${time1}ms, 数据量: ${categories1.length}`);
      
      // 第二次获取数据（应该从缓存获取）
      const startTime2 = Date.now();
      const categories2 = await categoryCacheService.getCategories();
      const time2 = Date.now() - startTime2;
      
      this.addResult('缓存获取分类数据', time2 < time1 && categories2.length === categories1.length, `耗时: ${time2}ms, 缓存加速: ${time1 - time2}ms`);
      
      // 测试更多菜单
      const moreMenu = await categoryCacheService.getMoreMenuItems();
      this.addResult('获取更多菜单数据', Array.isArray(moreMenu), `数据量: ${moreMenu.length}`);
      
    } catch (error) {
      this.addResult('缓存服务基本功能', false, `错误: ${error.message}`);
    }
  }

  /**
   * 测试数据一致性
   */
  async testDataConsistency() {
    
    try {
      // 获取多次数据，检查一致性
      const [data1, data2, data3] = await Promise.all([
        categoryCacheService.getCategories(),
        categoryCacheService.getCategories(),
        categoryCacheService.getCategories()
      ]);
      
      const isConsistent = JSON.stringify(data1) === JSON.stringify(data2) && 
                          JSON.stringify(data2) === JSON.stringify(data3);
      
      this.addResult('并发请求数据一致性', isConsistent, `三次请求结果${isConsistent ? '一致' : '不一致'}`);
      
    } catch (error) {
      this.addResult('数据一致性测试', false, `错误: ${error.message}`);
    }
  }

  /**
   * 测试错误处理
   */
  async testErrorHandling() {
    
    try {
      // 模拟网络错误（通过修改缓存配置）
      const originalTimeout = categoryCacheService.CACHE_CONFIG.NETWORK_TIMEOUT;
      categoryCacheService.CACHE_CONFIG.NETWORK_TIMEOUT = 1; // 1ms超时
      
      // 清理缓存强制从API获取
      categoryCacheService.clearCache();
      
      const categories = await categoryCacheService.getCategories();
      
      // 恢复原始配置
      categoryCacheService.CACHE_CONFIG.NETWORK_TIMEOUT = originalTimeout;
      
      this.addResult('网络错误降级处理', categories.length > 0, `降级后仍获取到${categories.length}条数据`);
      
    } catch (error) {
      this.addResult('错误处理测试', false, `错误: ${error.message}`);
    }
  }

  /**
   * 添加测试结果
   */
  addResult(testName, passed, details) {
    this.testResults.push({
      name: testName,
      passed,
      details,
      timestamp: new Date().toLocaleTimeString()
    });
  }

  /**
   * 打印测试结果
   */
  printResults() {
    const passedTests = this.testResults.filter(r => r.passed).length;
    const totalTests = this.testResults.length;

    return {
      summary: passedTests === totalTests ? '全部通过' : '存在失败用例',
      passed: passedTests,
      total: totalTests,
      details: this.testResults
    };
  }

  /**
   * 监控缓存性能
   */
  async monitorPerformance(duration = 30000) {
    const stats = {
      requests: 0,
      cacheHits: 0,
      totalTime: 0,
      errors: 0
    };
    
    const startTime = Date.now();
    
    while (Date.now() - startTime < duration) {
      try {
        const requestStart = Date.now();
        await categoryCacheService.getCategories();
        const requestTime = Date.now() - requestStart;
        
        stats.requests++;
        stats.totalTime += requestTime;
        
        if (requestTime < 50) { // 假设小于50ms为缓存命中
          stats.cacheHits++;
        }
        
        // 每秒请求一次
        await new Promise(resolve => setTimeout(resolve, 1000));
        
      } catch (error) {
        stats.errors++;
      }
    }
    
  }
}

// 导出测试实例
const categoryCache = new CategoryCacheTest();

// 在开发环境下自动运行测试
if (process.env.NODE_ENV === 'development') {
  // 延迟执行，等待应用初始化
  setTimeout(() => {
    categoryCache.runAllTests();
  }, 3000);
}

export default categoryCache;
