package com.campus.secondhand.mapper;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MyBatis Mapper XML 解析测试：不连接数据库，只验证 XML 合法、namespace 与接口绑定、语句齐全。
 *
 * <p>避免 XML 拼写错误直到启动/联调时才暴露；SQL 语义仍由 M3/M4 联调验证。</p>
 */
@DisplayName("MyBatis Mapper XML 解析测试（不依赖数据库）")
class MapperXmlParseTest {

    @Test
    @DisplayName("UserMapper.xml 可解析且 4 个语句齐全")
    void should_parseUserMapperXml_when_configurationLoads() throws Exception {
        Configuration configuration = parse("mapper/UserMapper.xml");

        assertThat(configuration.hasStatement("com.campus.secondhand.mapper.UserMapper.selectByUsername")).isTrue();
        assertThat(configuration.hasStatement("com.campus.secondhand.mapper.UserMapper.selectById")).isTrue();
        assertThat(configuration.hasStatement("com.campus.secondhand.mapper.UserMapper.countByUsername")).isTrue();
        assertThat(configuration.hasStatement("com.campus.secondhand.mapper.UserMapper.insert")).isTrue();
    }

    @Test
    @DisplayName("ItemMapper.xml 可解析且 6 个语句齐全（M3/M4 使用）")
    void should_parseItemMapperXml_when_configurationLoads() throws Exception {
        Configuration configuration = parse("mapper/ItemMapper.xml");

        assertThat(configuration.hasStatement("com.campus.secondhand.mapper.ItemMapper.selectById")).isTrue();
        assertThat(configuration.hasStatement("com.campus.secondhand.mapper.ItemMapper.selectList")).isTrue();
        assertThat(configuration.hasStatement("com.campus.secondhand.mapper.ItemMapper.countList")).isTrue();
        assertThat(configuration.hasStatement("com.campus.secondhand.mapper.ItemMapper.insert")).isTrue();
        assertThat(configuration.hasStatement("com.campus.secondhand.mapper.ItemMapper.update")).isTrue();
        assertThat(configuration.hasStatement("com.campus.secondhand.mapper.ItemMapper.deleteById")).isTrue();
    }

    /**
     * 用独立 Configuration 解析 XML：解析过程只做语法/映射校验，不建立数据库连接。
     */
    private Configuration parse(String resource) throws Exception {
        Configuration configuration = new Configuration();
        configuration.setMapUnderscoreToCamelCase(true);
        try (InputStream inputStream = Resources.getResourceAsStream(resource)) {
            XMLMapperBuilder builder = new XMLMapperBuilder(
                    inputStream, configuration, resource, configuration.getSqlFragments());
            builder.parse();
        }
        return configuration;
    }
}
