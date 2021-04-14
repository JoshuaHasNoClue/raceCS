import React from 'react';
import Fetch from './Fetch';
import aircs from './aircs.svg';
import './App.css';

import Leaderboard from './Leaderboard';
import Players from './Players';
import Stations from './Stations';
import NotificationDrawer from './NotificationDrawer';
import Wsh from './wsh';
import Ticker from './ticker';

class App extends React.Component {
  constructor(props) {
    super(props);
    
    this.state = {
      currentView: "leaderboard",
      state: "load",
      selectPlayer: null,
      playerData: {},
      stationData: {},
      recentEvents: []
    };
  }

  componentDidMount() {
    //Set up the WS connection
    let ws = new Wsh(`ws://${window.location.host}/ws`);
    ws.on("message", (data) => {
      switch (data.type) {
        case "newPlayer":
          this.setState(state => {
            let pdata = JSON.parse(JSON.stringify(state.playerData));
            pdata[data.user] = {
              "uuid": data.uuid,
              "visited": []
            };

            return {
              playerData: pdata
            }
          })
          break;
        case "removePlayer":
          this.setState(state => {
            let pdata = JSON.parse(JSON.stringify(state.playerData));
            delete pdata[data.user];
            
            return {
              playerData: pdata
            }
          })
          break;
        case "visitation":
          this.setState(state => {
            let pdata = JSON.parse(JSON.stringify(state.playerData));
            pdata[data.user].visited.push(data.station);
            
            return {
              playerData: pdata
            }
          });
      }
    });
    ws.on("open", () => {
      this.setState({
        state: "ready"
      });
    })
    ws.on("close", () => {
      this.setState({
        state: "error"
      });
    })

    this.setState({
      ws: ws
    });

    (async () => {
      this.setState({
        stationData: await Fetch.get("/stations"),
        playerData: await Fetch.get("/users")
      });
    })();
  }

  componentWillUnmount() {
    this.state.ws.close();
  }

  renderMainView() {
    switch (this.state.currentView) {
      case "leaderboard":
        return <Leaderboard stationData={this.state.stationData} playerData={this.state.playerData} recentEvents={this.state.recentEvents} onPlayerClicked={this.playerClicked.bind(this)}/>
      case "players":
        return <Players stationData={this.state.stationData} playerData={this.state.playerData} selectPlayer={this.state.selectPlayer} />
      case "stations":
        return <Stations stationData={this.state.stationData} playerData={this.state.playerData} onPlayerClicked={this.playerClicked.bind(this)} />
    }
  }

  playerClicked(player) {
    this.setState({
      currentView: "players",
      selectPlayer: player
    });
  }

  changeView(view) {
    this.setState({
      selectPlayer: null,
      currentView: view
    });
  }

  notificationPosted(notification) {
    this.setState(state => {
      return {
        recentEvents: [...state.recentEvents, notification]
      }
    });
  }

  renderState() {
    switch (this.state.state) {
      case "load":
        return <div>
          Loading...
        </div>
      case "ready":
        return <>
        <Ticker websocket={this.state.ws} />
          {this.renderMainView()}
          <div className="header">
            <div className={`headerButton ${this.state.currentView == "leaderboard" && "selected"}`} onClick={this.changeView.bind(this, "leaderboard")}>Leaderboard</div>
            <div className={`headerButton ${this.state.currentView == "stations" && "selected"}`} onClick={this.changeView.bind(this, "stations")}>Stations</div>
            <div className={`headerButton ${this.state.currentView == "players" && "selected"}`}onClick={this.changeView.bind(this, "players")}>Players</div>
            <div style={{flexGrow: 1}}></div>
            <img src={aircs} style={{height: "100%", padding: "9px", boxSizing: "border-box"}}></img>
          </div>
          <NotificationDrawer stationData={this.state.stationData} websocket={this.state.ws} onNotification={this.notificationPosted.bind(this)} />
        </>
        case "error":
          return <div className="errorContainer">
            <h1>Disconnected</h1>
            <p>Please reload the page</p>
          </div>
    }
  }

  render() {
    return <div className="mainContainer">
      {this.renderState()}
    </div>
  }
}

export default App;
