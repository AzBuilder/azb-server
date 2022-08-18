import { React ,useState} from "react";
import {  Layout, Breadcrumb, Tabs} from "antd";
import './Settings.css';
import { GeneralSettings } from "./General";
import { TeamSettings } from "./Teams"
import { VCSSettings } from "./VCS";
import { TemplatesSettings } from "./Templates";
import {ORGANIZATION_NAME} from '../../config/actionTypes';
import { SSHKeysSettings } from "./SSHKeys";
const { Content } = Layout;
const { TabPane } = Tabs;



export const OrganizationSettings = ({ selectedTab ,vcsMode}) => {
  const [key, setKey] = useState("");
  function callback(key) {
    setKey(key);
  }
  return (
    <Content style={{ padding: '0 50px' }}>
      <Breadcrumb style={{ margin: '16px 0' }}>
        <Breadcrumb.Item>{localStorage.getItem(ORGANIZATION_NAME)}</Breadcrumb.Item>
        <Breadcrumb.Item>Settings</Breadcrumb.Item>
      </Breadcrumb>
      <div className="site-layout-content">
        <Tabs tabPosition="left"  defaultActiveKey={selectedTab} onTabClick={callback}>
          <TabPane tab="General"  key="1">
            <GeneralSettings />
          </TabPane>
          <TabPane tab="Teams"  key="2">
            <TeamSettings />
          </TabPane>
          <TabPane tab="VCS Providers"  key="3">
            <VCSSettings vcsMode={vcsMode} />
          </TabPane>
          <TabPane tab="Templates" key="4">
            <TemplatesSettings key={key} />
          </TabPane>
          <TabPane tab="SSH Keys" key="5">
            <SSHKeysSettings />
          </TabPane>
        </Tabs>
      </div>
    </Content>
  );
}