import React from 'react';
import ReactDOM from 'react-dom';
import ServiceList from "./ServiceList"
import './App.css';

class App extends React.Component {

  constructor(props) {
    super(props);
    this.state = {
      services: []
    }
  }

  componentDidMount() {
    fetch("/api/services/status")
      .then(response => response.text())
      .then(text => this.setState({services: JSON.parse(text)}));
  }

  render() {
    return (
      <div className="App">
        <h1>Services</h1>
        <ServiceList services={this.state.services} />
      </div>
    )
  }

}

ReactDOM.render(
  <App/>,
  document.getElementById('root')
);
